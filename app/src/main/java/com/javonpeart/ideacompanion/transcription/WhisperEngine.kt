package com.javonpeart.ideacompanion.transcription

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteOrder
import kotlin.math.roundToInt

interface TranscriptionEngine { suspend fun transcribe(audioFile: File, modelFile: File): String }

class WhisperEngine : TranscriptionEngine {
    companion object { init { System.loadLibrary("idea_whisper") } }
    private external fun transcribeNative(modelPath: String, samples: FloatArray): String?
    override suspend fun transcribe(audioFile: File, modelFile: File): String = withContext(Dispatchers.Default) {
        require(modelFile.isFile && modelFile.length() > 1024) { "Import a valid Whisper model first" }
        val pcm = decode(audioFile)
        require(pcm.isNotEmpty()) { "Recording contains no decodable audio" }
        transcribeNative(modelFile.absolutePath, pcm)?.trim() ?: error("Whisper inference failed")
    }

    private fun decode(file: File): FloatArray {
        val extractor = MediaExtractor()
        extractor.setDataSource(file.absolutePath)
        try {
            val track = (0 until extractor.trackCount).firstOrNull { extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true }
                ?: error("No audio track")
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
            codec.configure(format, null, null, 0)
            codec.start()
            try {
                val samples = ArrayList<Float>()
                val info = MediaCodec.BufferInfo()
                var inputDone = false
                var outputDone = false
                var rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                while (!outputDone) {
                    if (!inputDone) {
                        val index = codec.dequeueInputBuffer(10000)
                        if (index >= 0) {
                            val buffer = codec.getInputBuffer(index)!!
                            val size = extractor.readSampleData(buffer, 0)
                            if (size < 0) { codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM); inputDone = true }
                            else { codec.queueInputBuffer(index, 0, size, extractor.sampleTime, 0); extractor.advance() }
                        }
                    }
                    when (val index = codec.dequeueOutputBuffer(info, 10000)) {
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            rate = codec.outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                            channels = codec.outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                            val encoding = if (codec.outputFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) codec.outputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING) else android.media.AudioFormat.ENCODING_PCM_16BIT
                            require(encoding == android.media.AudioFormat.ENCODING_PCM_16BIT) { "Unsupported PCM encoding" }
                        }
                        else -> if (index >= 0) {
                            val bytes = codec.getOutputBuffer(index)!!.duplicate().order(ByteOrder.LITTLE_ENDIAN)
                            bytes.position(info.offset); bytes.limit(info.offset + info.size)
                            val shorts = bytes.slice().order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                            while (shorts.remaining() >= channels) {
                                var sum = 0f
                                repeat(channels) { sum += shorts.get() / 32768f }
                                samples.add(sum / channels)
                            }
                            outputDone = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                            codec.releaseOutputBuffer(index, false)
                        }
                    }
                }
                val outputSize = (samples.size.toLong() * 16000 / rate).toInt()
                return FloatArray(outputSize) { index ->
                    val source = index.toDouble() * rate / 16000.0
                    val left = source.toInt().coerceIn(0, samples.lastIndex)
                    val right = (left + 1).coerceAtMost(samples.lastIndex)
                    (samples[left] * (1 - (source - left)) + samples[right] * (source - left)).toFloat()
                }
            } finally { codec.stop(); codec.release() }
        } finally { extractor.release() }
    }
}

#include <jni.h>
#include <string>
#include "whisper.h"

extern "C" JNIEXPORT jstring JNICALL
Java_com_javonpeart_ideacompanion_transcription_WhisperEngine_transcribeNative(
    JNIEnv *env, jobject, jstring modelPath, jfloatArray audio) {
    const char *path = env->GetStringUTFChars(modelPath, nullptr);
    auto params = whisper_context_default_params();
    whisper_context *ctx = whisper_init_from_file_with_params(path, params);
    env->ReleaseStringUTFChars(modelPath, path);
    if (!ctx) return nullptr;
    jfloat *samples = env->GetFloatArrayElements(audio, nullptr);
    const jsize count = env->GetArrayLength(audio);
    auto full = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    full.language = "en";
    full.translate = false;
    full.no_context = true;
    full.print_progress = false;
    full.print_realtime = false;
    full.print_timestamps = false;
    full.n_threads = 4;
    int rc = whisper_full(ctx, full, samples, count);
    env->ReleaseFloatArrayElements(audio, samples, JNI_ABORT);
    std::string result;
    if (rc == 0) {
        for (int i = 0; i < whisper_full_n_segments(ctx); ++i)
            result += whisper_full_get_segment_text(ctx, i);
    }
    whisper_free(ctx);
    return rc == 0 ? env->NewStringUTF(result.c_str()) : nullptr;
}

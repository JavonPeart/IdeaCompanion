#include <jni.h>
#include "llama.h"
#include <algorithm>
#include <cmath>
#include <stdexcept>
#include <string>
#include <vector>

static void fail(JNIEnv * env, const char * message) {
    jclass klass = env->FindClass("java/lang/IllegalStateException");
    env->ThrowNew(klass, message);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_javonpeart_ideacompanion_conversation_LocalGgufEngine_generateNative(
        JNIEnv * env, jobject, jstring path_arg, jstring prompt_arg) {
    const char * path = env->GetStringUTFChars(path_arg, nullptr);
    const char * prompt_chars = env->GetStringUTFChars(prompt_arg, nullptr);
    const std::string prompt(prompt_chars);
    llama_backend_init();
    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0;
    llama_model * model = llama_model_load_from_file(path, model_params);
    env->ReleaseStringUTFChars(path_arg, path);
    env->ReleaseStringUTFChars(prompt_arg, prompt_chars);
    if (!model) { fail(env, "Could not load the GGUF model"); return nullptr; }

    llama_context * context = nullptr;
    llama_batch_ext * batch = nullptr;
    llama_sampler * sampler = nullptr;
    std::string answer;
    std::string error;
    do {
        const llama_vocab * vocab = llama_model_get_vocab(model);
        const char * tmpl = llama_model_chat_template(model, nullptr);
        llama_chat_message message {"user", prompt.c_str()};
        int32_t size = llama_chat_apply_template(tmpl, &message, 1, true, nullptr, 0);
        if (size <= 0 || size > 16000) { error = "Unsupported or oversized model chat template"; break; }
        std::string formatted(size, '\0');
        if (llama_chat_apply_template(tmpl, &message, 1, true, formatted.data(), size) < 0) {
            error = "Could not format model prompt"; break;
        }
        // LFM3's official template uses an empty think block when thinking is disabled.
        // The simple llama chat-template API does not expose enable_thinking=false.
        if (formatted.size() >= 7 && formatted.compare(formatted.size() - 7, 7, "<think>") == 0) {
            formatted.append("</think>");
        } else if (formatted.size() < 15 || formatted.compare(formatted.size() - 15, 15, "<think></think>") != 0) {
            formatted.append("<think></think>");
        }
        int32_t count = -llama_tokenize(vocab, formatted.c_str(), formatted.size(), nullptr, 0, true, true);
        if (count <= 0 || count > 2048) { error = "Prompt is too long for phone memory"; break; }
        std::vector<llama_token> tokens(count);
        if (llama_tokenize(vocab, formatted.c_str(), formatted.size(), tokens.data(), count, true, true) < 0) {
            error = "Could not tokenize model prompt"; break;
        }
        llama_context_params params = llama_context_default_params();
        params.n_ctx = 2560;
        params.n_batch = 512;
        params.n_threads = 4;
        context = llama_init_from_model(model, params);
        if (!context) { error = "Could not allocate model context"; break; }
        batch = llama_batch_ext_init(context);
        llama_sampler_chain_params sample_params = llama_sampler_chain_default_params();
        sampler = llama_sampler_chain_init(sample_params);
        llama_token think_token[4];
        int32_t think_count = llama_tokenize(vocab, "<think>", 7, think_token, 4, false, true);
        if (think_count == 1) {
            llama_logit_bias bias {think_token[0], -INFINITY};
            llama_sampler_chain_add(sampler, llama_sampler_init_logit_bias(llama_vocab_n_tokens(vocab), 1, &bias));
        }
        llama_sampler_chain_add(sampler, llama_sampler_init_greedy());
        int32_t position = 0;
        for (int32_t offset = 0; offset < count; offset += 512) {
            int32_t end = std::min(count, offset + 512);
            llama_batch_ext_clear(batch);
            for (int32_t i = offset; i < end; ++i) {
                int32_t idx = llama_batch_ext_add_token(batch, 0, tokens[i]);
                llama_pos pos = position++;
                llama_batch_ext_set_pos(batch, idx, &pos);
            }
            llama_batch_ext_set_output_logits(batch, end - offset - 1, true);
            if (llama_process(context, LLAMA_PROCESS_TYPE_DECODE, batch) != 0) {
                error = "Model prompt evaluation failed"; break;
            }
        }
        if (!error.empty()) break;
        for (int32_t i = 0; i < 160; ++i) {
            llama_token token = llama_sampler_sample(sampler, context, -1);
            if (llama_vocab_is_eog(vocab, token)) break;
            char piece[512];
            int32_t length = llama_token_to_piece(vocab, token, piece, sizeof(piece), 0, true);
            if (length < 0) { error = "Model output could not be decoded"; break; }
            answer.append(piece, length);
            llama_batch_ext_clear(batch);
            int32_t idx = llama_batch_ext_add_token(batch, 0, token);
            llama_pos pos = position++;
            llama_batch_ext_set_pos(batch, idx, &pos);
            llama_batch_ext_set_output_logits(batch, 0, true);
            if (llama_process(context, LLAMA_PROCESS_TYPE_DECODE, batch) != 0) {
                error = "Model generation failed"; break;
            }
        }
    } while (false);
    if (sampler) llama_sampler_free(sampler);
    if (batch) llama_batch_ext_free(batch);
    if (context) llama_free(context);
    llama_model_free(model);
    if (!error.empty()) { fail(env, error.c_str()); return nullptr; }
    return env->NewStringUTF(answer.c_str());
}

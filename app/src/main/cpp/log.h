//
// Created by philip on 2022/12/5.
//
#include <android/log.h>

#ifndef TOFMODULE_LOG_H
#define TOFMODULE_LOG_H

#define LOG_SWITCH 0
#define LOG_TAG "TofModule"
#define LOGE(format, ...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, format, ##__VA_ARGS__)
#if(LOG_SWITCH == 1)
/*#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)*/
#define LOGV(format, ...)  __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, format, ##__VA_ARGS__)
#define LOGD(format, ...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, format, ##__VA_ARGS__)
#define LOGI(format, ...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, format, ##__VA_ARGS__)
#define LOGW(format, ...)  __android_log_print(ANDROID_LOG_WARN, LOG_TAG, format, ##__VA_ARGS__)
#else
#define LOGV(...) NULL
	#define LOGD(...) NULL
	#define LOGI(...) NULL
	#define LOGW(...) NULL
	//#define LOGE(...) NULL
#endif


#endif //TOFMODULE_LOG_H

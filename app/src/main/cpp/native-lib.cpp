#include <jni.h>
#include <string>
#include <unistd.h>
#include <signal.h>
#include <dlfcn.h>

#include <stdio.h>
#include <string.h>

#include "vl53l5cx_api.h"
#include "platform.h"
#include "log.h"

#define VL53L5CX_API_REVISION			"VL53L5CX_1.3.5"



//VL53L5CX_ResultsData 	Results;		/* Results data from VL53L5CX */
//uint8_t 				status, loop, isAlive, isReady, i;
VL53L5CX_Configuration 	Dev;

typedef struct{
    int frequencyHz;
    int resolutionValue;
    int targetOrder;
    int rangingMode;
    int integrationTimeMs;
    char device[16];
    char data[64];
} ParamInfo;

extern "C"
JNIEXPORT jobject JNICALL
Java_com_nbd_autofocus_tofmodule_TofHelper_getResultsData(JNIEnv *env, jobject thiz) {
    // TODO: implement getResultsData()
    VL53L5CX_ResultsData 	Results;		/* Results data from VL53L5CX */
    uint8_t 				status, loop, isReady, i;
    LOGD("vl53l5cx  start ranging \n");
    status = vl53l5cx_start_ranging(&Dev);
    if(status)
    {
        LOGE("vl53l5cx Start Ranging failed\n");
    }
    LOGD("vl53l5cx resolution %d streamcount %d \n", Dev.resolution, Dev.streamcount);
    time_t timep;
    loop = 0;
    short loopCount = 1;
    while(loop < loopCount) {

        time(&timep);  /*得到time_t类型的UTC时间*/
        LOGD("time():%d\n",timep);
        LOGD("vl53l5cx resolution %d streamcount %d \n", Dev.resolution, Dev.streamcount);
        vl53l5cx_check_data_ready(&Dev, &isReady);
        if (isReady) {
            vl53l5cx_get_ranging_data(&Dev, &Results);

            /* As the sensor is set in 4x4 mode by default, we have a total
             * of 16 zones to print. For this example, only the data of first zone are
             * print */
/*             LOGI("Print data no : %3u\n", Dev.streamcount);
           for (i = 0; i < 16; i++) {
                LOGI("Zone : %3d, Status : %3u, Distance : %4d mm\n",
                     i,
                     Results.target_status[VL53L5CX_NB_TARGET_PER_ZONE * i],
                     Results.distance_mm[VL53L5CX_NB_TARGET_PER_ZONE * i]);
            }
            printf("\n");*/
            loop++;
        }
        if (loopCount > 1)
            WaitMs(&Dev.platform, 5);
    }
    status = vl53l5cx_stop_ranging(&Dev);
    if(status)
    {
        LOGE("vl53l5cx Stop Ranging failed\n");
    }
    LOGD("vl53l5cx  Read Data End \n");

    //获取Java中的实例类ParamInfo
    jclass jcInfo = env->FindClass("com/nbd/autofocus/tofmodule/TofHelper$ResultsData");
    //byte[] array
    jfieldID jDistance = env->GetFieldID(jcInfo, "distanceMm", "[S");
    jfieldID jTargetStatus = env->GetFieldID(jcInfo, "targetStatus", "[B");
    //创建新的对象
    jobject joInfo = env->AllocObject(jcInfo);
#if false
    //数组赋值
    for (i = 0; i < Dev.resolution; i++) {
        LOGI("Naive Zone : %3d, Status : %3u, Distance : %4d mm\n",
             i,
             Results.target_status[VL53L5CX_NB_TARGET_PER_ZONE * i],
             Results.distance_mm[VL53L5CX_NB_TARGET_PER_ZONE * i]);
    }
#endif
    uint16_t  elementsLength = sizeof(Results.distance_mm)/sizeof(Results.distance_mm[0]);
    LOGD("Results distance_mm  length  %d  sizeof(jshort) %d \n",elementsLength, sizeof(jshort));
    jshortArray jia = env->NewShortArray(elementsLength);
    jshort *jshortArrayElements = env->GetShortArrayElements(jia, 0);
    memcpy(jshortArrayElements, Results.distance_mm, elementsLength*sizeof(jshort));
    env->SetShortArrayRegion(jia, 0, elementsLength, jshortArrayElements);
    env->SetObjectField(joInfo, jDistance, jia);

    jshortArrayElements[i];


    jbyteArray jba = env->NewByteArray(elementsLength);
    jbyte *jbyteArrayElements = env->GetByteArrayElements(jba, 0);
    memcpy(jbyteArrayElements, Results.target_status, elementsLength);
    env->SetByteArrayRegion(jba, 0, elementsLength, jbyteArrayElements);
    env->SetObjectField(joInfo, jTargetStatus, jba);

    return joInfo;
}

extern "C"
JNIEXPORT jbyte JNICALL
Java_com_nbd_autofocus_tofmodule_TofHelper_setParamInfo(JNIEnv *env, jobject thiz,
                                                        jobject param_info_set) {
    // TODO: implement setParamInfo()
    uint8_t 				status;

    ParamInfo paramInfo;

    //获取Java中的实例类ParamInfo
    jclass jcInfo = env->FindClass("com/nbd/autofocus/tofmodule/TofHelper$ParamInfo");
    //获取类中每一个变量的定义
    //int intValue
    jfieldID jfi = env->GetFieldID(jcInfo, "resolutionValue", "I");
    //获取实例的变量intValue的值
    paramInfo.resolutionValue = env->GetIntField(param_info_set, jfi);
    LOGI("Native paramInfo.intValue=%d \n",    paramInfo.resolutionValue);
    if ((paramInfo.resolutionValue != VL53L5CX_RESOLUTION_4X4) && (paramInfo.resolutionValue != VL53L5CX_RESOLUTION_8X8)) {
        paramInfo.resolutionValue = VL53L5CX_RESOLUTION_4X4;
    }

    /*********************************/
    /*        Set some params        */
    /*********************************/

    /* Set resolution in 8x8. WARNING : As others settings depend to this
     * one, it must be the first to use.
     */
    status = vl53l5cx_set_resolution(&Dev, paramInfo.resolutionValue);
    if(status)
    {
        LOGE("vl53l5cx_set_resolution failed, status %u\n", status);
        return status;
    }
/*    uint8_t resolution;
    vl53l5cx_get_resolution(&Dev, &resolution);*/

    /* Set ranging frequency to 10Hz.
     * Using 4x4, min frequency is 1Hz and max is 60Hz
     * Using 8x8, min frequency is 1Hz and max is 15Hz
     */
    status = vl53l5cx_set_ranging_frequency_hz(&Dev, 10);
    if(status)
    {
        LOGE("vl53l5cx_set_ranging_frequency_hz failed, status %u\n", status);
        return status;
    }

    /* Set target order to closest */
/*    status = vl53l5cx_set_target_order(&Dev, VL53L5CX_TARGET_ORDER_CLOSEST);
    if(status)
    {
        LOGE("vl53l5cx_set_target_order failed, status %u\n", status);
    }*/

    /* Get current integration time */
/*    uint32_t 				integration_time_ms;
    status = vl53l5cx_get_integration_time_ms(&Dev, &integration_time_ms);
    if(status)
    {
        LOGE("vl53l5cx_get_integration_time_ms failed, status %u\n", status);
    }
    LOGD("Current integration time is : %d ms\n", integration_time_ms);*/
    /*********************************/
    /*  Set ranging mode autonomous  */
    /*********************************/

/*    status = vl53l5cx_set_ranging_mode(&Dev, VL53L5CX_RANGING_MODE_CONTINUOUS);
    if(status)
    {
        printf("vl53l5cx_set_ranging_mode failed, status %u\n", status);
    }*/

    /* Using autonomous mode, the integration time can be updated (not possible
     * using continuous) */
    //status = vl53l5cx_set_integration_time_ms(&Dev, 20);
    return status;
}

extern "C"
JNIEXPORT jbyte JNICALL
Java_com_nbd_autofocus_tofmodule_TofHelper_openModule(JNIEnv *env, jobject thiz) {
    // TODO: implement openModule()

    uint8_t 				status, isAlive;
    /*********************************/
    /*   Power on sensor and init    */
    /*********************************/
    LOGD("vl53l5cx  init  \n");
    /* Initialize channel com */
    status = vl53l5cx_comms_init(&Dev.platform);
    if(status)
    {
        LOGE("VL53L5CX comms init failed\n");
        return status;
    }

    /* (Optional) Check if there is a VL53L5CX sensor connected */
    status = vl53l5cx_is_alive(&Dev, &isAlive);
    if(!isAlive || status)
    {
        LOGE("VL53L5CX not detected at requested address\n");
        return status;
    }

    /* (Mandatory) Init VL53L5CX sensor */
    status = vl53l5cx_init(&Dev);
    if(status)
    {
        LOGE("VL53L5CX ULD Loading failed\n");
        return status;
    }


    LOGD("VL53L5CX ULD ready ! (Version : %s)\n",
         VL53L5CX_API_REVISION);
    return 0;
}




extern "C" JNIEXPORT jstring JNICALL
Java_com_nbd_tofmodule_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello VL53L5CX";
    return env->NewStringUTF(hello.c_str());
}
/*

extern "C"
JNIEXPORT void JNICALL
Java_com_nbd_tofmodule_MainActivity_setInfo(JNIEnv *env, jobject thiz, jobject param_info_set) {
    // TODO: implement setInfo()

        ParamInfo paramInfo;

        //获取Java中的实例类ParamInfo
        jclass jcInfo = env->FindClass("com/nbd/tofmodule/MainActivity$ParamInfo");
        //获取类中每一个变量的定义
        //boolean boolValue
        jfieldID jfb = env->GetFieldID(jcInfo, "boolValue", "Z");
        //char charValue
        jfieldID jfc = env->GetFieldID(jcInfo, "charValue", "C");
        //double charValue
        jfieldID jfd = env->GetFieldID(jcInfo, "doubleValue", "D");
        //int intValue
        jfieldID jfi = env->GetFieldID(jcInfo, "intValue", "I");
        //byte[] array
        jfieldID jfa = env->GetFieldID(jcInfo, "array", "[B");
        //String str
        jfieldID jfs = env->GetFieldID(jcInfo, "str", "Ljava/lang/String;");

        //获取实例的变量boolValue的值
        paramInfo.boolValue = env->GetBooleanField(param_info_set, jfb);
        //获取实例的变量charValue的值
        paramInfo.charValue = env->GetCharField(param_info_set, jfc);
        //获取实例的变量doubleValue的值
        paramInfo.doubleValue = env->GetDoubleField(param_info_set, jfd);
        //获取实例的变量intValue的值
        paramInfo.intValue = env->GetIntField(param_info_set, jfi);
        //获取实例的变量array的值
        jbyteArray ja = (jbyteArray)env->GetObjectField(param_info_set, jfa);
        int  nArrLen = env->GetArrayLength(ja);
        char *chArr = (char*)env->GetByteArrayElements(ja, 0);
        memcpy(paramInfo.array, chArr, nArrLen);
        //获取实例的变量str的值
        jstring jstr = (jstring)env->GetObjectField(param_info_set, jfs);
        const char* pszStr = (char*)env->GetStringUTFChars(jstr, 0);
        strcpy(paramInfo.str, pszStr);

        //日志输出
        LOGI("paramInfo.array=%s, paramInfo.boolValue=%d, paramInfo.charValue=%c\n",
             paramInfo.array, paramInfo.boolValue, paramInfo.charValue);
        LOGI("paramInfo.doubleValue=%lf, paramInfo.intValue=%d,  paramInfo.str=%s\n",
             paramInfo.doubleValue, paramInfo.intValue, paramInfo.str);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_nbd_tofmodule_MainActivity_getInfo(JNIEnv *env, jobject thiz) {
    // TODO: implement getInfo()

    char chTmp[] = "Test array";
    int nTmpLen = strlen(chTmp);
    //将C结构体转换成Java类
    ParamInfo paramInfo;
    memset(paramInfo.array, 0, sizeof(paramInfo.array));
    memcpy(paramInfo.array, chTmp, strlen(chTmp));
    paramInfo.boolValue = true;
    paramInfo.charValue = 'B';
    paramInfo.doubleValue = 2.7182;
    paramInfo.intValue = 8;
    strcpy(paramInfo.str, "Hello from JNIv philip ");

    LOGI("paramInfo.array=%s, paramInfo.boolValue=%d, paramInfo.charValue=%c\n",
         paramInfo.array, paramInfo.boolValue, paramInfo.charValue);

    //获取Java中的实例类
    jclass jcInfo = env->FindClass("com/nbd/tofmodule/MainActivity$ParamInfo");

    //获取类中每一个变量的定义
    //boolean boolValue
    jfieldID jfb = env->GetFieldID(jcInfo, "boolValue", "Z");
    //char charValue
    jfieldID jfc = env->GetFieldID(jcInfo, "charValue", "C");
    //double doubleValue
    jfieldID jfd = env->GetFieldID(jcInfo, "doubleValue", "D");
    //int intValue
    jfieldID jfi = env->GetFieldID(jcInfo, "intValue", "I");
    //byte[] array
    jfieldID jfa = env->GetFieldID(jcInfo, "array", "[B");
    //String str
    jfieldID jfs = env->GetFieldID(jcInfo, "str", "Ljava/lang/String;");

    //创建新的对象
    jobject joInfo = env->AllocObject(jcInfo);

    //给类成员赋值
    env->SetBooleanField(joInfo, jfb, paramInfo.boolValue);
    env->SetCharField(joInfo, jfc, (jchar)paramInfo.charValue);
    env->SetDoubleField(joInfo, jfd, paramInfo.doubleValue);
    env->SetIntField(joInfo, jfi, paramInfo.intValue);

    //数组赋值
    jbyteArray jarr = env->NewByteArray(nTmpLen);
    jbyte *jby = env->GetByteArrayElements(jarr, 0);
    memcpy(jby, paramInfo.array, nTmpLen);
    env->SetByteArrayRegion(jarr, 0, nTmpLen, jby);
    env->SetObjectField(joInfo, jfa, jarr);

    //字符串赋值
    jstring jstrTmp = env->NewStringUTF(paramInfo.str);
    env->SetObjectField(joInfo, jfs, jstrTmp);

    return joInfo;
}
*/
#define LOG_TAG "LogicDroid"

#include "JNIHelp.h"
#include "JniConstants.h"
#include "JniException.h"

#include <unistd.h>

#include <sys/syscall.h>

/*
  For emulator :
  - 361 is the syscall number for check event
  - 362 is the syscall number for init monitor
  - 363 is the syscall number for change predicate
*/

static jboolean LogicDroid_checkEvent(JNIEnv*, jobject, jint caller, jint target)
{
	  //return (jboolean)syscall(376, caller, target);
	  return (jboolean)syscall(361, caller, target);
}

static void LogicDroid_initializeMonitor(JNIEnv* mEnv, jobject, jintArray UID) {
	  jsize len = mEnv->GetArrayLength(UID);
	  jint *bufferUID = mEnv->GetIntArrayElements(UID, 0);
	  int argUID[len];
	  int i;
	  for (i = 0; i < len; i++)
	  {
		    argUID[i] = (int)bufferUID[i];
	  }
	  //syscall(377, argUID, len);
	  syscall(362, argUID, len);
	  mEnv->ReleaseIntArrayElements(UID, bufferUID, 0);
}

// In this case, we know that there will only be one variable
static void LogicDroid_modifyStaticVariable(JNIEnv*, jobject, jint UID, jboolean value, jint rel) {
	  //syscall(378, rel, (int)UID, (char)value);
	  syscall(363, rel, (int)UID, (char)value);
}

static JNINativeMethod gMethods[] = {
	NATIVE_METHOD(LogicDroid, initializeMonitor, "([I)V"),
	NATIVE_METHOD(LogicDroid, modifyStaticVariable, "(IZI)V"),
	NATIVE_METHOD(LogicDroid, checkEvent, "(II)Z")
};

void register_java_security_LogicDroid(JNIEnv* env) {
    jniRegisterNativeMethods(env, "java/security/LogicDroid", gMethods, NELEM(gMethods));
}

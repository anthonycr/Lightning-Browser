#!/bin/bash
#
# build GMP and libjbigi.so using the Android tools directly
#
# WARNING:
# BROKEN - not updated for r19 NDK, aarch64, or GMP 6.1.2
# Use the following in i2p.i2p source core/c/jbigi:
# TARGET=android BITS=32 mbuild_all.sh
# TARGET=android BITS=64 mbuild_all.sh
#
# TODO: Get more settings from environment variables set in ../custom-rules.xml
#

# uncomment to skip
# exit 0

## works on linux and other unixes, but not osx.
if [ "`uname -s`" != "Darwin" ]; then
    THISDIR=$(dirname $(readlink -ne $0))
else
    THISDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
fi
cd $THISDIR

I2PBASE=${1:-$THISDIR/../../../../../i2p.i2p}
if [ ! -d "$I2PBASE" ]; then
    echo "I2P source not found in $I2PBASE"
    if [ -z "$1" ]; then
        echo "Extract it there or provide a path:"
        echo "./build.sh path/to/i2p.i2p"
    else
        echo "Extract it there or fix the supplied path"
    fi
    exit 1
fi

ROUTERJARS=$THISDIR/../../../../routerjars

## Check the local.properties file first
export NDK=$(awk -F= '/ndk\.dir/{print $2}' "$ROUTERJARS/local.properties")

if [ "$NDK" == "" ]; then
## Simple fix for osx development
    if [ `uname -s` = "Darwin" ]; then
        export NDK="/Developer/android/ndk/"
    else
#
# We want to be able to not have to update this script
# every time a new NDK comes out. We solve this by using readlink with
# a wild card, deglobbing automatically sorts to get the highest revision.
# the dot at the end ensures that it is a directory, and not a file.
#
        NDK_GLOB=$THISDIR/'../../../../../android-ndk-r*/.'
        export NDK="`readlink -n -e $(for last in $NDK_GLOB; do true; done ; echo $last)`"
    fi

    if [ "$NDK" == "" ]; then
        echo "Cannot find NDK in $NDK_GLOB"
        echo "Install it here, or set ndk.dir in $ROUTERJARS/local.properties, or adjust NDK_GLOB in script"
        exit 1
    fi
fi

if [ ! -d "$NDK" ]; then
    echo "Cannot find NDK in $NDK, install it"
    exit 1
fi

JBIGI="$I2PBASE/core/c/jbigi"
#
# GMP Version
#
# prelim stats on a droid
# java (libcrypto) 29 ms
# 4.3.2 (jbigi) 34 ms
# 5.0.2 (jbigi) 32 ms
# libcrypto crashes on emulator, don't trust it
# jbigi about 20-25% slower than java on emulator
#
GMPVER=6.1.2
GMP="$JBIGI/gmp-$GMPVER"

if [ ! -d "$GMP" ]; then
    echo "Source dir for GMP version $GMPVER not found in $GMP"
    echo "Install it there or change GMPVER and/or GMP in this script"
    exit 1
fi

# Apply necessary patch
patch -d $GMP -p1 <gmp_thumb_add_mssaaaa.patch

if [ `uname -s` = "Darwin" ]; then
    export JAVA_HOME=$(/usr/libexec/java_home)
else
    [ -z $JAVA_HOME ] && . $I2PBASE/core/c/find-java-home
fi
if [ ! -f "$JAVA_HOME/include/jni.h" ]; then
    echo "Cannot find jni.h! Looked in '$JAVA_HOME/include/jni.h'"
    echo "Please set JAVA_HOME to a java home that has the JNI"
    exit 1
fi

#
# API level, pulled from client build.gradle
#
LEVEL=$(awk -F' ' '/minSdkVersion/{print $2}' ../../../build.gradle)

#
# 4.6 is the GCC version. GCC 4.4.3 support was removed in NDK r9b.
# Available in r10:
#	arm-linux-androideabi-4.6
#	arm-linux-androideabi-4.8
#	arm-linux-androideabi-clang3.3
#	arm-linux-androideabi-clang3.4
#	llvm-3.3
#	llvm-3.4
#	mipsel-linux-android-4.6
#	mipsel-linux-android-4.8
#	mipsel-linux-android-clang3.3
#	mipsel-linux-android-clang3.4
#	x86-4.6
#	x86-4.8
#	x86-clang3.3
#	x86-clang3.4
GCCVER=4.8

for ABI in "armeabi" "armeabi-v7a" "x86" "mips"; do

# ABI-specific settings
case "$ABI" in
    "armeabi" | "armeabi-v7a")
        ARCH="arm"
        TOOLCHAIN="arm-linux-androideabi-$GCCVER"
        export BINPREFIX="arm-linux-androideabi-"
        CONFIGUREHOST="arm-linux-androideabi"
        ;;
    "x86")
        ARCH="x86"
        TOOLCHAIN="x86-$GCCVER"
        export BINPREFIX="i686-linux-android-"
        CONFIGUREHOST="i686-linux-android"
        ;;
    "mips")
        ARCH="mips"
        TOOLCHAIN="mipsel-linux-android-$GCCVER"
        export BINPREFIX="mipsel-linux-android-"
        CONFIGUREHOST="mipsel-linux-android"
        ;;
esac

if [ ! -e $PWD/$ABI ]
then
    mkdir $PWD/$ABI
fi

LIBFILE=$PWD/$ABI/libjbigi.so
if [ -f $LIBFILE ]
then
    echo "$LIBFILE exists, nothing to do here"
    echo "If you wish to force a recompile, delete it"
    continue
fi

if [ `uname -s` = "Darwin" ]; then
    export SYSTEM="darwin-x86"
    BUILDHOST="x86-darwin"
elif [ `uname -m` = "x86_64" ]; then
    export SYSTEM="linux-x86_64"
    BUILDHOST="x86_64-pc-linux-gnu"
else
    export SYSTEM="linux-x86"
    BUILDHOST="x86-pc-linux-gnu"
fi

TOOLCHAINDIR="/tmp/android-$LEVEL-$ARCH-$GCCVER"
if [ ! -d ${TOOLCHAINDIR} ]
then
    ${NDK}/build/tools/make-standalone-toolchain.sh --toolchain=${TOOLCHAIN} --platform=android-${LEVEL} --install-dir=${TOOLCHAINDIR} --system=${SYSTEM}
fi

export SYSROOT="$TOOLCHAINDIR/sysroot/"
if [ ! -d "$SYSROOT" ]; then
    echo "Cannot find $SYSROOT in NDK, check for support of level: $LEVEL arch: $ARCH or adjust LEVEL and ARCH in script"
    exit 1
fi

COMPILER="$TOOLCHAINDIR/bin/${BINPREFIX}gcc"
if [ ! -f "$COMPILER" ]; then
    echo "Cannot find compiler $COMPILER in NDK, check for support of system: $SYSTEM ABI: $AABI or adjust AABI and SYSTEM in script"
    exit 1
fi
export CC="$COMPILER --sysroot=$SYSROOT"
# worked without this on 4.3.2, but 5.0.2 couldn't find it
export NM="$TOOLCHAINDIR/bin/${BINPREFIX}nm"
STRIP="$TOOLCHAINDIR/bin/${BINPREFIX}strip"

export LIBGMP_LDFLAGS='-avoid-version'

case "$ARCH" in
    "arm")
        BASE_CFLAGS='-O2 -g -pedantic -fomit-frame-pointer -Wa,--noexecstack -ffunction-sections -funwind-tables -fstack-protector -fno-strict-aliasing -finline-limit=64'
        case "$ABI" in
            "armeabi")
                MPN_PATH="arm/v5 arm generic"
                export CFLAGS="${BASE_CFLAGS} -march=armv5te -mtune=xscale -msoft-float -mthumb"
                ;;
            "armeabi-v7a")
                MPN_PATH="arm/v6t2 arm/v6 arm/v5 arm generic"
                export CFLAGS="${BASE_CFLAGS} -march=armv7-a -mfloat-abi=softfp -mfpu=vfp"
                ;;
        esac
        export LDFLAGS='-Wl,--fix-cortex-a8 -Wl,--no-undefined -Wl,-z,noexecstack -Wl,-z,relro -Wl,-z,now'
        ;;
    "x86")
        MPN_PATH="x86/atom/sse2 x86/atom/mmx x86/atom x86 generic"
        # Base CFLAGS set from ndk-build output
        BASE_CFLAGS='-O2 -g -pedantic -Wa,--noexecstack -fomit-frame-pointer -ffunction-sections -funwind-tables -fstrict-aliasing -funswitch-loops -finline-limit=300'
        # x86, CFLAGS set according to 'CPU Arch ABIs' in the r8c documentation
        export CFLAGS="${BASE_CFLAGS} -march=i686 -mtune=atom -msse3 -mstackrealign -mfpmath=sse -m32"
        export LDFLAGS='-Wl,-z,noexecstack,-z,relro'
        ;;
    "mips")
        MPN_PATH=""
        # Base CFLAGS set from ndk-build output
        BASE_CFLAGS='-O2 -g -pedantic -fomit-frame-pointer -Wa,--noexecstack -fno-strict-aliasing -finline-functions -ffunction-sections -funwind-tables -fmessage-length=0 -fno-inline-functions-called-once -fgcse-after-reload -frerun-cse-after-loop -frename-registers -funswitch-loops -finline-limit=300'
        # mips CFLAGS not specified in 'CPU Arch ABIs' in the r8b documentation
        export CFLAGS="${BASE_CFLAGS}"
        export LDFLAGS='-Wl,--no-undefined -Wl,-z,noexecstack -Wl,-z,relro -Wl,-z,now'
        ;;
esac

#echo "CC is $CC"

mkdir -p build
cd build

# we must set both build and host, so that the configure
# script will set cross_compile=yes, so that it
# won't attempt to run the a.out files
if [ ! -f config.status ]; then
    echo "Configuring GMP..."
    if [ -z "$MPN_PATH" ]; then
        $GMP/configure --with-pic --build=$BUILDHOST --host=$CONFIGUREHOST || exit 1
    else
        $GMP/configure --with-pic --build=$BUILDHOST --host=$CONFIGUREHOST MPN_PATH="$MPN_PATH" || exit 1
    fi
fi

echo "Building GMP..."
make -j8 || exit 1

COMPILEFLAGS="-fPIC -Wall $CFLAGS"
INCLUDES="-I. -I$JBIGI/jbigi/include -I$JAVA_HOME/include -I$JAVA_HOME/include/linux"
LINKFLAGS="-shared -Wl,-soname,libjbigi.so $LDFLAGS"

echo "Building jbigi lib that is statically linked to GMP"
STATICLIBS=".libs/libgmp.a"

echo "Compiling C code..."
rm -f jbigi.o $LIBFILE
echo "$CC -c $COMPILEFLAGS $INCLUDES $JBIGI/jbigi/src/jbigi.c"
$CC -c $COMPILEFLAGS $INCLUDES $JBIGI/jbigi/src/jbigi.c || exit 1
echo "$CC $LINKFLAGS $INCLUDES -o $LIBFILE jbigi.o $STATICLIBS"
$CC $LINKFLAGS $INCLUDES -o $LIBFILE jbigi.o $STATICLIBS || exit 1
echo "$STRIP $LIBFILE"
$STRIP $LIBFILE || exit 1

ls -l $LIBFILE || exit 1

cd ..
rm -r build

echo 'Built successfully'

done

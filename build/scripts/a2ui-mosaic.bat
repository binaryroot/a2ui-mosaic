@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem
@rem SPDX-License-Identifier: Apache-2.0
@rem

@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem
@rem  a2ui-mosaic startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
@rem This is normally unused
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and A2UI_MOSAIC_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH. 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME% 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

goto fail

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\a2ui-mosaic-0.1.0.jar;%APP_HOME%\lib\mosaic-runtime-jvm-0.18.0.jar;%APP_HOME%\lib\lifecycle-runtime-compose-desktop-2.9.2.jar;%APP_HOME%\lib\runtime-desktop-1.8.2.jar;%APP_HOME%\lib\kotlinx-serialization-core-jvm-1.7.3.jar;%APP_HOME%\lib\kotlinx-serialization-json-jvm-1.7.3.jar;%APP_HOME%\lib\mosaic-tty-terminal-jvm-0.18.0.jar;%APP_HOME%\lib\mosaic-terminal-jvm-0.18.0.jar;%APP_HOME%\lib\finalization-hook-jvm-0.1.0.jar;%APP_HOME%\lib\lifecycle-runtime-desktop-2.9.2.jar;%APP_HOME%\lib\lifecycle-common-jvm-2.9.2.jar;%APP_HOME%\lib\kotlinx-coroutines-core-jvm-1.10.2.jar;%APP_HOME%\lib\kotlin-codepoints-jvm.jar;%APP_HOME%\lib\poko-annotations-jvm-0.19.3.jar;%APP_HOME%\lib\atomicfu-jvm-0.23.2.jar;%APP_HOME%\lib\collection-jvm-1.5.0.jar;%APP_HOME%\lib\cite-api-jvm-0.6.1.jar;%APP_HOME%\lib\mosaic-tty-jvm-0.18.0.jar;%APP_HOME%\lib\core-common-2.2.0.jar;%APP_HOME%\lib\annotation-jvm-1.9.1.jar;%APP_HOME%\lib\kotlin-stdlib-2.2.10.jar;%APP_HOME%\lib\annotations-23.0.0.jar;%APP_HOME%\lib\jspecify-1.0.0.jar


@rem Execute a2ui-mosaic
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %A2UI_MOSAIC_OPTS%  -classpath "%CLASSPATH%" org.a2ui.mosaic.sample.MainKt %*

:end
@rem End local scope for the variables with windows NT shell
if %ERRORLEVEL% equ 0 goto mainEnd

:fail
rem Set variable A2UI_MOSAIC_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
set EXIT_CODE=%ERRORLEVEL%
if %EXIT_CODE% equ 0 set EXIT_CODE=1
if not ""=="%A2UI_MOSAIC_EXIT_CONSOLE%" exit %EXIT_CODE%
exit /b %EXIT_CODE%

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega

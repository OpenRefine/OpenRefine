@echo off
rem Previous line hides the remarks from being displayed with the prompt for each line

rem Change current working directory to directory of the batch script
cd %~dp0

rem
rem Configuration variables
rem
rem MAVEN_HOME
rem   Home of Maven installation; copy is in the source as tools\apache-ant-*
rem
rem JAVA_HOME
rem   Home of Java installation.
rem
rem JAVA_OPTIONS
rem   Extra options to pass to the JVM
rem

if "%OS%"=="Windows_NT" @setlocal
if "%OS%"=="WINNT" @setlocal

rem --- First two utilities for exiting --------------------------------------------

goto endUtils

:usage
echo Usage %0 [options] ^<action^>
echo where [options] include:
echo.
echo  "/?" print this message and exit
echo.
echo  "/p <port>" the port that OpenRefine will listen to
echo     default: 3333
echo.
echo  "/i <interface>" the network interface OpenRefine should bind to
echo     default: 127.0.0.1
echo.
echo  "/H <host>" the expected value for the Host header (set to * to disable checks)
echo     default: ^<interface^>
echo.
echo  "/w <path>" path to the webapp
echo     default src\main\webapp
echo.
echo  "/d" enable JVM debugging (on port 8000)
echo.
echo  "/m <memory>" max memory heap size to use
echo     default: 1400M
echo.
echo  "/x" enable JMX monitoring (for jconsole and friends)
echo.
echo  "/c <path>" path to the refine.ini file
echo     default .\refine.ini
echo.
echo "and <action> is one of
echo.
echo   build ..................... Build OpenRefine
echo   run ....................... Run OpenRefine (using only "refine" or "./refine" will also start OpenRefine)
echo.
echo   test ...................... Run all the tests
echo   server_test ............... Run the server tests
echo   extensions_test ........... Run the extensions tests
echo.

echo   clean ..................... Clean compiled classes
echo.
goto end

:fail
echo Type 'refine /h' for usage.
goto end

:endUtils

set OPTS=

:endConfigReading
														 
rem --- Argument parsing --------------------------------------------

:loop
if ""%1"" == """" goto readIniFile
if ""%1"" == ""/?"" goto usage
if ""%1"" == ""/h"" goto usage
if ""%1"" == ""/p"" goto arg-p
if ""%1"" == ""/i"" goto arg-i
if ""%1"" == ""/H"" goto arg-H
if ""%1"" == ""/w"" goto arg-w
if ""%1"" == ""/d"" goto arg-d
if ""%1"" == ""/m"" goto arg-m
if ""%1"" == ""/x"" goto arg-x
if ""%1"" == ""/c"" goto arg-c
goto readIniFile

:arg-p
set REFINE_PORT=%2
goto shift2loop

:arg-i
set REFINE_INTERFACE=%2
goto shift2loop

:arg-H
set REFINE_HOST=%2
goto shift2loop

:arg-w
set REFINE_WEBAPP=%2
goto shift2loop

:arg-m
set REFINE_MEMORY=%2
set REFINE_MIN_MEMORY=%2
goto shift2loop

:arg-d
set OPTS=%OPTS% -Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n
goto shift2loop

:arg-x
set OPTS=%OPTS% -Dcom.sun.management.jmxremote
goto shift2loop

:arg-c
set REFINE_INI_PATH=%~2
goto shift2loop

:shift2loop
shift
shift
goto loop

:readIniFile

rem --- Read ini file -----------------------------------------------

if "%REFINE_INI_PATH%" == "" set REFINE_INI_PATH=refine.ini
if not exist %REFINE_INI_PATH% (
	echo The system cannot find the file %REFINE_INI_PATH%
	exit /B 1
)
echo Using %REFINE_INI_PATH% for configuration
for /f "tokens=1,* delims==" %%a in (%REFINE_INI_PATH%) do (
    set %%a=%%b
)

:endArgumentParsing

rem --- Check JAVA_HOME ---------------------------------------------

if not "%JAVA_HOME%" == "" goto gotJavaHome
echo You must set JAVA_HOME to point at your Java Development Kit installation
echo.
echo If you don't know how to do this, follow the instructions at
echo.
echo   http://bit.ly/1c2gkR
echo.

goto fail
:gotJavaHome			

rem --- Fold in Environment Vars --------------------------------------------

if not "%JAVA_OPTIONS%" == "" goto gotJavaOptions
set JAVA_OPTIONS=
:gotJavaOptions
set OPTS=%OPTS% %JAVA_OPTIONS%

if not "%REFINE_MEMORY%" == "" goto gotMemory
set REFINE_MEMORY=1400M
if not "%REFINE_MIN_MEMORY%" == "" goto gotMemory
set REFINE_MIN_MEMORY=256M

:gotMemory
set OPTS=%OPTS% -Xms%REFINE_MIN_MEMORY% -Xmx%REFINE_MEMORY% -Drefine.memory=%REFINE_MEMORY%

if not "%REFINE_MAX_FORM_CONTENT_SIZE%" == "" goto gotMaxFormContentSize
set REFINE_MAX_FORM_CONTENT_SIZE=1048576
:gotMaxFormContentSize
set OPTS=%OPTS% -Drefine.max_form_content_size=%REFINE_MAX_FORM_CONTENT_SIZE%

if not "%REFINE_PORT%" == "" goto gotPort
set REFINE_PORT=3333
:gotPort
set OPTS=%OPTS% -Drefine.port=%REFINE_PORT%

if not "%REFINE_INTERFACE%" == "" goto gotInterface
set REFINE_INTERFACE=127.0.0.1
:gotInterface
set OPTS=%OPTS% -Drefine.interface=%REFINE_INTERFACE%

if not "%REFINE_HOST%" == "" goto gotHost
if "%REFINE_INTERFACE%" == "" goto skipHost
set REFINE_HOST=%REFINE_INTERFACE%
:gotHost
set OPTS=%OPTS% -Drefine.host=%REFINE_HOST%
:skipHost

if not "%REFINE_WEBAPP%" == "" goto gotWebApp
set REFINE_WEBAPP=main\webapp
:gotWebApp
set OPTS=%OPTS% -Drefine.webapp=%REFINE_WEBAPP%

if not "%REFINE_CLASSES_DIR%" == "" goto gotClassesDir
set REFINE_CLASSES_DIR=server\classes
:gotClassesDir

if not "%REFINE_LIB_DIR%" == "" goto gotLibDir
set REFINE_LIB_DIR=server\target\lib
:gotLibDir

if "%GDATA_CLIENT_ID%" == "" goto skipGDataCredentials
if "%GDATA_CLIENT_SECRET%" == "" goto skipGDataCredentials
if "%GDATA_API_KEY%" == "" goto skipGDataCredentials
set OPTS=%OPTS% -Dext.gdata.clientid=%GDATA_CLIENT_ID% -Dext.gdata.clientsecret=%GDATA_CLIENT_SECRET% -Dext.gdata.apikey=%GDATA_API_KEY%
:skipGDataCredentials

rem ----- Respond to the action ----------------------------------------------------------

set ACTION=%1
setlocal
%@Try%
if ""%ACTION%"" == ""build"" goto doMvn
if ""%ACTION%"" == ""server_test"" goto doMvn
if ""%ACTION%"" == ""extensions_test"" goto doMvn
if ""%ACTION%"" == ""test"" goto doMvn
if ""%ACTION%"" == ""clean"" goto doMvn
if ""%ACTION%"" == ""run"" goto doRun
if ""%ACTION%"" == """" goto doRun
%@EndTry%
:@Catch
  echo Unknown Refine command called "%1", type "refine /?" for proper usage.
  exit /B 1
:@EndCatch

:doRun
rem --- Log for troubleshooting ------------------------------------------
set JAVA="%JAVA_HOME%/bin/java"
set JAVA_VERSION=""
set JAVA_RELEASE=0
for /f "tokens=3" %%g in ('^"%JAVA% -version 2^>^&1 ^| findstr /i "version"^"') do (
  set JAVA_VERSION=%%g
)
rem Java 6, 7, 8 starts with 1.x
rem Java 9+ starts with x using semver versioning
set JAVA_VERSION=%JAVA_VERSION:"=%
for /f "delims=.-_ tokens=1-2" %%v in ("%JAVA_VERSION%") do (
  if /I "%%v" EQU "1" (
    set JAVA_RELEASE=%%w
  ) else (
    set JAVA_RELEASE=%%v
  )
)
echo Java %JAVA_RELEASE% (%JAVA_VERSION%)
if %JAVA_RELEASE% LSS 11 (
    echo OpenRefine requires Java version 11 or later. If you have multiple versions of Java installed, please set the environment variable JAVA_HOME to the correct version.
    exit /B 1
)
if %JAVA_RELEASE% GTR 17 (
    echo WARNING: OpenRefine is not tested and not recommended for use with Java versions greater than 17.
)

echo Getting Free Ram...
for /f "tokens=2 delims=:" %%i in ('systeminfo ^| findstr /C:"Available Physical Memory"') do (set freeRam=%%i)
(
echo ----------------------- 
echo PROCESSOR_ARCHITECTURE = %PROCESSOR_ARCHITECTURE%
echo JAVA_HOME = %JAVA_HOME%
echo java release = %JAVA_RELEASE%
echo java -version = %JAVA_VERSION%
echo freeRam = %freeRam%
echo REFINE_MEMORY = %REFINE_MEMORY%
echo ----------------------- 
) > support.log

set CLASSPATH="%REFINE_CLASSES_DIR%;%REFINE_LIB_DIR%\*"
%JAVA% -cp %CLASSPATH% %OPTS% -Djava.library.path=%REFINE_LIB_DIR%/native/windows com.google.refine.Refine
goto end

:doMvn
if not "%MAVEN_HOME%" == "" goto gotMvnHome
echo You must have Apache Maven installed and the MAVEN_HOME environment variable to point to it.
echo.
echo You can download it from
echo.
echo   https://maven.apache.org/
echo
echo The environment variable MAVEN_HOME should not include the final "bin" directory, such as:
echo
echo   C:\Program Files (x86)\Apache\Maven
echo.
echo If you don't know how to set environment variables, follow the instructions at
echo.
echo   http://bit.ly/1c2gkR
echo.
:gotMvnHome
set MVN_ACTION=""%ACTION%""
if ""%ACTION%"" == ""build"" goto :build-setup
goto :endif
:build-setup
pushd main\webapp 
call npm install
popd
set MVN_ACTION=compile test-compile dependency:build-classpath
:endif

if ""%ACTION%"" == ""test"" set MVN_ACTION=test dependency:build-classpath
if ""%ACTION%"" == ""server_test"" set MVN_ACTION=test -f main
if ""%ACTION%"" == ""extensions_test"" set MVN_ACTION=test -f extensions
call "%MAVEN_HOME%\bin\mvn.cmd" process-resources
call "%MAVEN_HOME%\bin\mvn.cmd" %MVN_ACTION%
goto end

:end

@echo off
setlocal EnableDelayedExpansion

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
echo Usage: %0 [options] ^<action^>
echo.
echo Options
echo.    /?                      Print this message and exit.
echo     /c ^<path^>               Path to the refine.ini file. Default: .\refine.ini.
echo     /d ^<path^>               Path to the data directory. Default: OS dependent.
echo     /debug                  Enable JVM debugging (on port 8000).
echo     /H ^<host^>               Expected host header value (* to disable checks). Default: ^<interface^>.
echo     /i ^<interface^>          The network interface OpenRefine should bind to. Default: 127.0.0.1.
echo     /jmx                    Enable JMX monitoring.
echo     /m ^<memory^>             Set JVM min and max memory heap size to use. Default: 1400M.
echo     /p ^<port^>               The port that OpenRefine should bind to. Default: 3333.
echo     /v ^<level^>              Verbosity level [error, warn, info, debug, trace].
echo     /w ^<path^>               Path to the webapp. Default: src\main\webapp.
echo     /x ^<name=value^>         Additional configuration parameters to pass to OpenRefine.
echo.
echo Actions
echo     build                   Build OpenRefine.
echo     clean                   Clean compiled classes.
echo     extensions_test         Run the extensions tests.
echo     run                     Run OpenRefine.
echo     server_test             Run the server tests.
echo     test                    Run all the tests.
echo     lint                    Reformat the source code according to OpenRefine's conventions.
goto :eof



:fail
echo Type 'refine /h' for usage.
goto :eof

:endUtils

set OPTS=

rem ----- Check for custom ini file /c option  --------------------------------

set "REFINE_INI_PATH="
set "FOUND_C="
for %%A in (%*) do (
    if defined FOUND_C (
        set REFINE_INI_PATH=%%A
        goto :readIniFile
    ) else if "%%A"=="/c" (
        set "FOUND_C=1"
    )
)

:readIniFile
rem --- Read ini file ---------------------------------------------------------
if "!REFINE_INI_PATH!" == "" set REFINE_INI_PATH=refine.ini
if not exist !REFINE_INI_PATH! (
 echo Error: "!REFINE_INI_PATH!" does not exist.
 exit /b 1
)
for /f "usebackq tokens=1,* delims== " %%a in (`type "!REFINE_INI_PATH!" ^| findstr /R /B /V "^# ^; ^$"`) do (
    set "%%a=%%b"
)
														 
rem ----- Parse the command line args -----------------------------------------

:loop
if "%~1"=="" goto checkVars
if "%~1"=="/?" goto usage
if "%~1"=="/h" goto usage
if "%~1"=="/p" set "REFINE_PORT=%~2" & shift & shift & goto loop
if "%~1"=="/i" set "REFINE_INTERFACE=%~2" & shift & shift & goto loop
if "%~1"=="/H" set "REFINE_HOST=%~2" & shift & shift & goto loop
if "%~1"=="/w" set "REFINE_WEBAPP=%~2" & shift & shift & goto loop
if "%~1"=="/m" set "REFINE_MEMORY=%~2" & set "REFINE_MIN_MEMORY=%~2" & shift & shift & goto loop
if "%~1"=="/d" set "REFINE_DATA_DIR=%~2" & shift & shift & goto loop
if "%~1"=="/debug" set "OPTS=%OPTS% -Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n" & shift & goto loop
if "%~1"=="/x" set "REFINE_EXTRA_OPTS=%~2" & shift & shift & goto loop
if "%~1"=="/jmx" set "OPTS=%OPTS% -Dcom.sun.management.jmxremote" & shift & goto loop
if "%~1"=="/c" shift & shift & goto loop
if "%~1"=="/v" set "REFINE_VERBOSITY=%~2" & shift & shift & goto loop

:checkVars

rem --- Check JAVA_HOME -------------------------------------------------------

if not "%JAVA_HOME%" == "" goto gotJavaHome
echo You must set JAVA_HOME to point at your Java Development Kit installation
echo.
echo If you don't know how to do this, follow the instructions at
echo.
echo   https://openrefine.org/docs/manual/installing#java
echo.

goto fail
:gotJavaHome			

rem ----- Verify and Set Required Environment Variables -----------------------

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

if not "%REFINE_DATA_DIR%" == "" set OPTS=%OPTS% -Drefine.data_dir=%REFINE_DATA_DIR%

if not "%REFINE_LIB_DIR%" == "" goto gotLibDir
set REFINE_LIB_DIR=server\target\lib
:gotLibDir

if not "%REFINE_VERBOSITY%" == "" goto gotVerbosity
:gotVerbosity
set OPTS=%OPTS% -Drefine.verbosity=%REFINE_VERBOSITY%

if not "%REFINE_EXTRA_OPTS%" == "" set OPTS=%OPTS% -D%REFINE_EXTRA_OPTS%

if "%GDATA_CLIENT_ID%" == "" goto skipGDataCredentials
if "%GDATA_CLIENT_SECRET%" == "" goto skipGDataCredentials
if "%GDATA_API_KEY%" == "" goto skipGDataCredentials
set OPTS=%OPTS% -Dext.gdata.clientid=%GDATA_CLIENT_ID% -Dext.gdata.clientsecret=%GDATA_CLIENT_SECRET% -Dext.gdata.apikey=%GDATA_API_KEY%
:skipGDataCredentials

rem ----- Respond to the action ----------------------------------------------------------

set ACTION=%1
setlocal
if ""%ACTION%"" == ""build"" goto doMvn
if ""%ACTION%"" == ""server_test"" goto doMvn
if ""%ACTION%"" == ""extensions_test"" goto doMvn
if ""%ACTION%"" == ""test"" goto doMvn
if ""%ACTION%"" == ""lint"" goto doMvn
if ""%ACTION%"" == ""clean"" goto doMvn
if ""%ACTION%"" == ""run"" goto doRun
if ""%ACTION%"" == """" goto doRun
  echo Unknown Refine command called "%1", type "refine /?" for proper usage.
  exit /B 1

:doRun

for /f "tokens=2 delims==" %%i in ('wmic OS get FreePhysicalMemory /Value') do set /a freeRam=%%i/1024
echo -------------------------------------------------------------------------------------------------
echo You have %freeRam%M of free memory.
echo Your current configuration is set to use %REFINE_MEMORY% of memory.
echo OpenRefine can run better when given more memory. Read our FAQ on how to allocate more memory here:
echo https://openrefine.org/docs/manual/installing\#increasing-memory-allocation
echo -------------------------------------------------------------------------------------------------

rem --- Checking Java Version  ------------------------------------------
set JAVA="%JAVA_HOME%\bin\java.exe"

if not exist !JAVA! (
    echo The specified path !JAVA! does not point to a valid Java Development Kit installation.
    echo Please check that the path is correct and that a Java Development Kit is installed at that location.
    echo   https://openrefine.org/docs/manual/installing#java
    goto :fail
)

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
if %JAVA_RELEASE% GTR 21 (
    echo WARNING: OpenRefine is not tested and not recommended for use with Java versions greater than 21.
)

set CLASSPATH="%REFINE_CLASSES_DIR%;%REFINE_LIB_DIR%\*"
%JAVA% -cp %CLASSPATH% %OPTS% -Djava.library.path=%REFINE_LIB_DIR%/native/windows com.google.refine.Refine
goto :eof

:doMvn
if defined MAVEN_HOME (
    set "MVN=%MAVEN_HOME%\bin\mvn"
) else if defined M2_HOME (
    set "MVN=%M2_HOME%\bin\mvn"
) else (
    set "MVN=mvn"
)

if not exist "%MVN%" (
    echo Apache Maven not found. Please set M2_HOME or MAVEN_HOME environment variables or ensure that 'mvn' is in your system PATH.
    exit /b 1
)
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
if ""%ACTION%"" == ""lint"" (
    set MVN_ACTION=formatter:format impsort:sort
    rem Skip the call to process-resources as it's not needed for this action
    goto :mvnCall
)
call "%MVN%" process-resources
:mvnCall
call "%MVN%" %MVN_ACTION%
goto :eof

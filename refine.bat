@echo off
rem
rem Configuration variables
rem
rem ANT_HOME
rem   Home of Ant installation; copy is in the source as tools\apache-ant-*
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
echo  "/i <interface>" the host interface OpenRefine should bind to
echo     default: 127.0.0.1
echo.
echo  "/w <path>" path to the webapp
echo     default src\main\webapp
echo.
echo  "/d" enable JVM debugging (on port 8000)
echo.
echo  "/m <memory>" max memory heap size to use
echo     default: 1024M
echo.
echo  "/x" enable JMX monitoring (for jconsole and friends)
echo.
echo "and <action> is one of
echo.
echo   build ..................... Build OpenRefine
echo   run ....................... Run OpenRefine
echo.
echo   server_test ............... Run the server tests
echo.

echo   clean ..................... Clean compiled classes
echo   distclean ................. Remove all generated files
echo.
goto end

:fail
echo Type 'refine /h' for usage.
goto end

:endUtils

rem --- Read ini file -----------------------------------------------

set OPTS=

for /f "tokens=1,* delims==" %%a in (refine.ini) do (
    set %%a=%%b
)

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

rem --- Argument parsing --------------------------------------------

:loop
if ""%1"" == """" goto endArgumentParsing
if ""%1"" == ""/h"" goto usage
if ""%1"" == ""/?"" goto usage
if ""%1"" == ""/p"" goto arg-p
if ""%1"" == ""/i"" goto arg-i
if ""%1"" == ""/w"" goto arg-w
if ""%1"" == ""/d"" goto arg-d
if ""%1"" == ""/m"" goto arg-m
if ""%1"" == ""/x"" goto arg-x
goto endArgumentParsing

:arg-p
set REFINE_PORT=%2
goto shift2loop

:arg-i
set REFINE_HOST=%2
goto shift2loop

:arg-w
set REFINE_WEBAPP=%2
goto shift2loop

:arg-m
set REFINE_MEMORY=%2
goto shift2loop

:arg-d
set OPTS=%OPTS% -Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n
goto shift2loop

:arg-x
set OPTS=%OPTS% -Dcom.sun.management.jmxremote
goto shift2loop

:shift2loop
shift
shift
goto loop

:endArgumentParsing

rem --- Fold in Environment Vars --------------------------------------------

if not "%JAVA_OPTIONS%" == "" goto gotJavaOptions
set JAVA_OPTIONS=
:gotJavaOptions
set OPTS=%OPTS% %JAVA_OPTIONS%

if not "%REFINE_MEMORY%" == "" goto gotMemory
set REFINE_MEMORY=1024M
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

if not "%REFINE_HOST%" == "" goto gotHost
set REFINE_HOST=127.0.0.1
:gotHost
set OPTS=%OPTS% -Drefine.host=%REFINE_HOST%

if not "%REFINE_WEBAPP%" == "" goto gotWebApp
set REFINE_WEBAPP=main\webapp
:gotWebApp
set OPTS=%OPTS% -Drefine.webapp=%REFINE_WEBAPP%

if not "%REFINE_CLASSES_DIR%" == "" goto gotClassesDir
set REFINE_CLASSES_DIR=server\classes
:gotClassesDir

if not "%REFINE_LIB_DIR%" == "" goto gotLibDir
set REFINE_LIB_DIR=server\lib
:gotLibDir

rem ----- Respond to the action ----------------------------------------------------------

set ACTION=%1

if ""%ACTION%"" == ""build"" goto doAnt
if ""%ACTION%"" == ""server_test"" goto doAnt
if ""%ACTION%"" == ""clean"" goto doAnt
if ""%ACTION%"" == ""distclean"" goto doAnt
if ""%ACTION%"" == ""run"" goto doRun

:doRun
set CLASSPATH="%REFINE_CLASSES_DIR%;%REFINE_LIB_DIR%\*"
"%JAVA_HOME%\bin\java.exe" -cp %CLASSPATH% %OPTS% -Djava.library.path=%REFINE_LIB_DIR%/native/windows com.google.refine.Refine
goto end

:doAnt
if not "%ANT_HOME%" == "" goto gotAntHome
echo You must have Apache Ant installed and the ANT_HOME environment variable to point to it
echo.
echo You can download it from
echo.
echo   http://ant.apache.org/
echo.
echo If you don't know how to set environment variables, follow the instructions at
echo.
echo   http://bit.ly/1c2gkR
echo.
:gotAntHome
"%ANT_HOME%\bin\ant.bat" -f build.xml %ACTION%
goto end

:end

@echo off
rem
rem Configuration variables
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
echo  /h print this message and exit
echo.
echo  /p <port> the port that Gridworks will listen to
echo     default: 3333
echo.
echo  /i <interface> the host interface gridworks should bind to
echo     default: 127.0.0.1
echo.
echo  /w <path> path to the webapp
echo     default src\main\webapp
echo.
echo  /d enable JVM debugging (on port 8000)
echo.
echo  /m <memory> max memory heap size to use
echo     default: 1024M
echo.
echo  /x enable JMX monitoring (for jconsole and friends)
echo.
echo and <action> is one of
echo.
echo   build ..................... Build Gridworks      
echo   run ....................... Run Gridworks
echo.
echo   clean ..................... Clean compiled classes
echo   distclean ................. Remove all generated files
echo.
goto end

:fail
echo Type 'gridworks /h' for usage.
goto end

:endUtils

if not "%JAVA_HOME%" == "" goto gotJavaHome
echo You must set JAVA_HOME to point at your Java Development Kit installation
echo.
echo If you don't know how to do this, follow the instructions at 
echo.
echo   http://bit.ly/1c2gkR
echo.

goto fail
:gotJavaHome

rem --- Read ini file --------------------------------------------

set OPTS=

for /f "tokens=1,2 delims==" %%a in (gridworks.ini) do ( 
    set %%a=%%b 
) 

rem --- Argument parsing --------------------------------------------

:loop
if ""%1"" == """" goto endArgumentParsing
if ""%1"" == ""/h"" goto usage
if ""%1"" == ""/p"" goto arg-p
if ""%1"" == ""/i"" goto arg-i
if ""%1"" == ""/w"" goto arg-w
if ""%1"" == ""/d"" goto arg-d
if ""%1"" == ""/m"" goto arg-m
if ""%1"" == ""/x"" goto arg-x
goto endArgumentParsing

:arg-p
set GRIDWORKS_PORT=%2
goto shift2loop

:arg-i
set GRIDWORKS_HOST=%2
goto shift2loop

:arg-w
set GRIDWORKS_WEBAPP=%2
goto shift2loop

:arg-m
set GRIDWORKS_MEMORY=%2
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

if not "%GRIDWORKS_MEMORY%" == "" goto gotMemory
set GRIDWORKS_MEMORY=1024M
:gotMemory
set OPTS=%OPTS% -Xms256M -Xmx%GRIDWORKS_MEMORY%

if not "%GRIDWORKS_PORT%" == "" goto gotPort
set GRIDWORKS_PORT=3333
:gotPort
set OPTS=%OPTS% -Dgridworks.port=%GRIDWORKS_PORT%

if not "%GRIDWORKS_HOST%" == "" goto gotHost
set GRIDWORKS_HOST=127.0.0.1
:gotHOST
set OPTS=%OPTS% -Dgridworks.host=%GRIDWORKS_HOST%

if not "%GRIDWORKS_WEBAPP%" == "" goto gotHost
set GRIDWORKS_WEBAPP=src\main\webapp
:gotHOST
set OPTS=%OPTS% -Dgridworks.webapp=%GRIDWORKS_WEBAPP%

if not "%GRIDWORKS_BUILD_DIR%" == "" goto gotBuildDir
set GRIDWORKS_BUILD_DIR=build
:gotBuildDir

if not "%GRIDWORKS_LIB_DIR%" == "" goto gotLibDir
set GRIDWORKS_LIB_DIR=lib
:gotLibDir

rem ----- Respond to the action ----------------------------------------------------------

set ACTION=%1

if ""%ACTION%"" == ""build"" goto doAnt
if ""%ACTION%"" == ""clean"" goto doAnt
if ""%ACTION%"" == ""distclean"" goto doAnt
if ""%ACTION%"" == ""run"" goto doRun

:doRun
set CLASSPATH="%GRIDWORKS_BUILD_DIR%\classes;%GRIDWORKS_LIB_DIR%\*"
"%JAVA_HOME%\bin\java.exe" -cp %CLASSPATH% %OPTS% -Djava.library.path=lib/native/windows com.metaweb.gridworks.Gridworks
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
"%ANT_HOME%\bin\ant.bat" -f build.xml -Dbuild.dir="%GRIDWORKS_BUILD_DIR%" %ACTION%
goto end

:end

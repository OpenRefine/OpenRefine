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

rem --- First two utilities for exiting --------------------------------------------

goto endUtils

:usage
echo Usage: %0 [options] <action>
echo where [options] include:
echo.
echo  /h print this message and exit
echo.
echo  /p <port> the port that Gridworks will listen to
echo     default: 3333
echo.
echo  /d enable JVM debugging (on port 8000)
echo.
echo  /s <mode> enable profiling
echo     (available modes are "hprof" and "yourkit")
echo.
echo  /x enable JMX monitoring (for jconsole and friends)
echo.
echo and <action> is one of
echo.
echo   run         Run Gridworks (default)
echo. 
echo   eclipse     Build Eclipse project files
echo.
echo.
goto end

:fail
echo See: '%0 /h' for usage.
goto end

:endUtils

if not "%JAVA_HOME%" == "" goto gotJavaHome
echo You must set JAVA_HOME to point at your Java Development Kit installation
goto fail
:gotJavaHome

set MAVEN_OPTS=%GRIDWORKS_OPTS% -Djava.awt.headless=true

rem --- Argument parsing --------------------------------------------

:loop
if ""%1"" == """" goto endArgumentParsing
if ""%1"" == ""/h"" goto usage
if ""%1"" == ""/p"" goto arg-p
if ""%1"" == ""/d"" goto arg-d
if ""%1"" == ""/s"" goto arg-s
if ""%1"" == ""/x"" goto arg-x
goto endArgumentParsing

:arg-p
set GRIDWORKS_PORT=%2
goto shift2loop

:arg-d
set MAVEN_OPTS=%MAVEN_OPTS% -Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n
goto shift2loop

:arg-s
set PROFILE_MODE=%2
if ""%PROFILE_MODE%"" == ""hprof"" goto profileWithHprof
if ""%PROFILE_MODE%"" == ""yourkit"" goto profileWithYourkit
goto shift2loop

:profileWithHprof
set MAVEN_OPTS=%MAVEN_OPTS% -Xrunhprof:heap=all,cpu=samples,thread=y,depth=3
goto shift2loop

:profileWithYourkit
set MAVEN_OPTS=%MAVEN_OPTS% -agentlib:yjpagent=sessionname=Gridworks
goto shift2loop

:arg-x
set MAVEN_OPTS=%MAVEN_OPTS% -Dcom.sun.management.jmxremote
goto shift2loop 

:shift2loop
shift
shift
goto loop

:endArgumentParsing

rem --- Fold in Environment Vars --------------------------------------------

if not "%JAVA_OPTIONS%" == "" goto gotJavaOptions
set JAVA_OPTIONS=-Xms32M -Xmx256M
:gotJavaOptions
set MAVEN_OPTS=%MAVEN_OPTS% %JAVA_OPTIONS%

if not "%GRIDWORKS_PORT%" == "" goto gotPort
set GRIDWORKS_PORT=3333
:gotPort
set MAVEN_OPTS=%MAVEN_OPTS% -Djetty.port=%GRIDWORKS_PORT%

rem ----- Respond to the action ----------------------------------------------------------

set ACTION=%1

if not "%ACTION%" == "" goto gotAction
set ACTION="run"
:gotJavaOptions
set MAVEN_OPTS=%MAVEN_OPTS% %JAVA_OPTIONS%

if ""%ACTION%"" == ""run"" goto doRun
if ""%ACTION%"" == ""eclipse"" goto doEclipse

:doRun
echo MAVEN_OPTS=%MAVEN_OPTS%
start http://127.0.0.1:%GRIDWORKS_PORT%/
mvn %MAVEN_PARAMS% jetty:run
goto end

:doEclipse
mvn eclipse:clean
mvn eclipse:eclipse
goto end

:end

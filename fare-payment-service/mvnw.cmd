@echo off
rem ----------------------------------------------------------------------------
rem Maven wrapper
rem ----------------------------------------------------------------------------
setlocal
set PRG=%~dpnx0
set WRAPPER_DIR=%~dp0\.mvn\wrapper
set WRAPPER_JAR=%WRAPPER_DIR%\maven-wrapper.jar

if not exist "%WRAPPER_JAR%" (
  echo Maven Wrapper JAR not found: %WRAPPER_JAR%
  echo Attempting to download the wrapper jar...
  for /f "usebackq tokens=1* delims==" %%i in ("%WRAPPER_DIR%\maven-wrapper.properties") do (
    if "%%i"=="wrapperUrl" set WRAPPER_URL=%%j
  )
  if not defined WRAPPER_URL (
    rem Fallback to a known wrapper URL if parsing failed
    set WRAPPER_URL=https://repo.maven.apache.org/maven2/io/takari/maven-wrapper/0.5.6/maven-wrapper-0.5.6.jar
  )
  if defined WRAPPER_URL (
    powershell -Command "try { (New-Object System.Net.WebClient).DownloadFile('%WRAPPER_URL%', '%WRAPPER_JAR%') } catch { exit 1 }" || (
      echo Failed to download wrapper jar. Please ensure network access or install Maven locally.
      exit /b 1
    )
  ) else (
    echo maven-wrapper.properties not found; cannot download wrapper jar.
    exit /b 1
  )
)

if defined JAVA_HOME (
  "%JAVA_HOME%\bin\java" -jar "%WRAPPER_JAR%" %*
) else (
  java -jar "%WRAPPER_JAR%" %*
)

@ECHO OFF
SETLOCAL enabledelayedexpansion

SET "target=android-21"

RMDIR external\appcompat /s /q
XCOPY "%ANDROID_HOME%\extras\android\support\v7\appcompat" "external\appcompat\*" /s /e /y /q

REM This library is already included by netcipher, but SHA1 of JARs differ
DEL /f /q external\appcompat\libs\android-support-v4.jar

RMDIR external\palette /s /q
XCOPY "%ANDROID_HOME%\extras\android\support\v7\palette" "external\palette\*" /s /e /y /q
MKDIR external\palette\src

REM Update ant setup in project and all sub-projects
SET "pattern=project.properties"
FOR /R "./external/" %%# in (*.properties) DO (
    ECHO %%~nx# | FIND "%pattern%" 1>NUL && (
        SET current_dir=%~d0%%~p#
        SET current_dir=!current_dir:\=/!
        ECHO Updating ant setup in "!current_dir!"
        CALL android update lib-project -t %target% -p "!current_dir!"
    )
)
CALL android update project -p . --subprojects -t %target% --name Lightning

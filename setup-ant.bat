@ECHO OFF
SETLOCAL enabledelayedexpansion

SET "target=android-20"

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

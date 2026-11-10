@echo off
cd /d "%~dp0"
set "GB=%ProgramFiles%\Git\bin\bash.exe"
if exist "%GB%" ( "%GB%" scripts/run.sh ) else ( bash scripts/run.sh )
pause

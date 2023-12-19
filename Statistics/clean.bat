REM go to the raw data folder
cd ./Data

REM and call the cleaning vim9script
gvim --cmd "source cleanupAll.vim | q"

REM alternatively use the "silent" before source to supress the gvim message
REM gvim --cmd "silent source cleanupAll.vim | q"

vim9script

# clean normal results
:%s/\vGraph_Dracula_|crossings!|It's a tie between |\s//ge
:%s/\vbeats|with|_|:/,/ge
:%s/\v([a-zA-Z])(\d)/\1,\2/ge
:%s/\d\+\.\d\+/\=printf('%.3f', str2float(submatch(0)))/ge

# clean ties
silent! g/and/norm $F,y$hp

# clean both
:%s/\(\d\+\),\(\d\+\)$/\= submatch(1) >= submatch(2) ? join([submatch(1), submatch(2)], ',') : join([submatch(2), submatch(1)], ',')/ge
sort

# reposition ties
silent! g/and/norm jmikddGp'i
:%s/and/,/ge

# clean errors
:%s/attemptedaninvalidmove\./,/ge
:%s/wins!/,-1,-1/ge
silent! g/-1/norm jmikddGp'i

# add .csv header
var signs = len(split(getline('.'), ',', 1)) - 1
if signs == 9
    norm ggOstra,perc,circ,samp,stra2,perc2,circ2,samp2,cros1,cros2
elseif signs == 11
    norm ggOstra,perc,circ,samp,vert,stra2,perc2,circ2,samp2,vert2,cros1,cros2
endif

# save and undo current file
w! %:r.csv
u

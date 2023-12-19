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
norm ggOstra,perc,circ,samp,stra2,perc2,circ2,samp2,cros1,cros2

# save and undo current file
w! %:r.csv
u

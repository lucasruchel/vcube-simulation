
#set key right bottom
set key right top


set style fill solid border rgb "black"

set yrange [0:*]

set terminal pdf enhanced


set datafile separator ';'

set ylabel "Mensagens"
set xlabel "Processos"

set style line 1 \
    linecolor rgb '#0060ad' \
    linetype 1 linewidth 2 \
    pointtype 7 pointsize 1.5

set output 'mensagens.pdf'

plot 'dados.csv' using 2:xtic(1) with linespoints linestyle 1

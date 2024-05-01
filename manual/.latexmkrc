@default_excluded_files = ('*-concordance.tex');

$pdflatex = 'pdflatex -synctex=1 -interaction=nonstopmode --shell-escape';
# $pdflatex = 'xelatex -synctex=1 %O %S -interaction=nonstopmode --shell-escape';
$pdf_previewer = "zathura %O %S";

$bibtex_use = 2;
$clean_ext = "nav snm vrb";
# $pdflatex="lualatex -interaction nonstopmode -shell-escape";

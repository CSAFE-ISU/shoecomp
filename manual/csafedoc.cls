\NeedsTeXFormat{LaTeX2e}

\ProvidesClass{csafedoc}[class following CSAFE Style Guide] % almost
\LoadClass[a4paper,oneside]{memoir}

\RequirePackage{iftex} % use LuaTeX, so font loading easier
\RequireLuaTeX
\RequirePackage{fontspec}

\IfFileExists{./fonts/Montserrat-Regular.ttf}{%
\setmainfont{Montserrat}[
    Path=./fonts/,
    Extension = .ttf,
    UprightFont=*-Regular,
    BoldFont=*-Bold,
    ItalicFont=*-Italic,
    BoldItalicFont=*-BoldItalic
]
\ClassWarning{csafedoc}{Using Montserrat font in ./fonts directory.}
}{\ClassWarning{csafedoc}{Could not find Montserrat font in ./fonts directory.}}

\RequirePackage{float} % for figure[H]
\RequirePackage{epsfig}
\RequirePackage{graphics}
\RequirePackage{tabularx}

\RequirePackage{xcolor} % custom colors
\definecolor{pantone425c}{HTML}{54585A}
\definecolor{pantone654c}{HTML}{003A70}
\definecolor{pantone637c}{HTML}{49bfe8}

\RequirePackage{hyperref} % for hyperlinks, within and outside the doc
\hypersetup{
    colorlinks,
    linkcolor={red!50!black},
    citecolor={blue!50!black},
    urlcolor=pantone654c,
}

\RequirePackage{caption} % for smaller captions
\DeclareCaptionFont{smallcaption}{\fontsize{8}{12}\selectfont}
\DeclareCaptionStyle{csafefigure}%
[margin=5mm,justification=centering]%
{font={smallcaption,color=pantone654c},labelfont=bf}
\captionsetup{style=csafefigure}

\RequirePackage[compact]{titlesec}
\titleformat{\chapter}[display]
{\fontsize{35}{35}\selectfont\bfseries\color{pantone425c}}{}{0pt}{}
\titlespacing*{\chapter}{0pt}{-50pt}{5pt}

\titleformat{\section}
  {\fontsize{21}{21}\selectfont\bfseries\color{pantone637c}}
  {\thesection}{1em}{}


% Page Layout
\settypeblocksize{24.7cm}{18cm}{*} % vertical, horizontal, ratio
\setlrmargins{*}{*}{1} % left, right, ratio
\setulmargins{*}{*}{1} % upper, lower, ratio
\setlength{\topmargin}{0pt}
\setlength{\headheight}{0pt}
\setlength{\headsep}{0pt}
\setlength{\footskip}{2cm}
\checkandfixthelayout

\makepagestyle{csafepage}
\makeevenhead{csafepage}{}{}{}
\makeoddhead{csafepage}{}{}{}

\renewcommand*{\chaptermark}[1]{\markboth{#1}{#1}}

\newcommand{\pagefooterLeft}{%
\begin{tabularx}{\linewidth}{c!{\color{pantone425c}\vrule}c!{\color{pantone425c}\vrule}c} 
\includegraphics[height=0.7cm,keepaspectratio]{images/csafe-logo.png} &
\includegraphics[height=0.7cm,keepaspectratio]{images/csafe-tools-logo.png} & 
\raisebox{.12cm}[0pt][0pt]{\includegraphics[height=0.5cm,keepaspectratio]{images/shoecomp-logo-words.png}}
\end{tabularx}
}
\newcommand{\pagefooterRight}{%
\vspace*{0.5cm}
\color{pantone654c}\leftmark\ \ \ \bfseries\thepage}

\makefootrule{csafepage}{\textwidth}{0.6pt}{7.5pt}
\makeheadfootruleprefix{csafepage}{}{\color{pantone654c}}
\makeevenfoot{csafepage}{\pagefooterLeft}{}{\pagefooterRight}
\makeoddfoot{csafepage}{\pagefooterLeft}{}{\pagefooterRight}

\aliaspagestyle{simple}{csafepage}
\aliaspagestyle{chapter}{csafepage}

\pagestyle{csafepage}

% Custom Commands
\bibliographystyle{IEEEtran}

\newcommand{\selfref}[1]{\texttt{\href{#1}{#1}}}
\newcommand{\rref}[2]{\texttt{\href{#2}{#1}}}

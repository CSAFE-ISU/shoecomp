# What is ShoeComp?

ShoeComp is a tool developed by CSAFE, to mark interest points on Shoeprint
images, align the images, and export to other viewing tools (like PhotoShop) or
scoring/comparison tools.

ShoeComp is built as a (macro+plugin) collection for [ImageJ2][ij2] aka
[FIJI][fiji]: the functionality for alignment and related operations is
implemented as a Java JAR, accessible via ImageJ plugins, and the GUI/markup
functionality is implemented via core ImageJ2 functions, macros, and third-party
plugins like [ActionBar][actionbar].

## What is the purpose of this repo?

The purpose of this repo is to provide scripts that consolidate all the
information required to package ImageJ into a Shoeprint Markup + Alignment GUI. 

This repo contains:

- scripts to download ImageJ (for different OS/arch combos)
- scripts to download the necessary plugins used in ShoeComp
- ImageJ macros containing necessary functionality to activate/run ShoeComp
- configuration files for ImageJ plugins
- icons and related assets
- A User Guide for ShoeComp -- the `manual` folder contains the TeX source for
  building the ShoeComp User Guide, along with a copy of TTF fonts from
  [Montserrat Font Family](https://fonts.google.com/specimen/Montserrat), which
  are available under the [SIL Open Font License](https://openfontlicense.org/).

At present we have a Github Actions runner that provides the ShoeComp plugin as
a Java JAR, and ZIP files of [FIJI][fiji] containing ShoeComp.

[ij2]: https://imagej.net/software/imagej2/
[fiji]: https://imagej.net/software/fiji/
[actionbar]: https://imagej.net/plugins/action-bar

<main>
<disableAltClose>
<disableCtrlAltEdit>
<hideMenus>
<noGrid>

<startupAction>
eval("js", "if(IJ.getInstance().isVisible()) IJ.getInstance().setVisible(false);");
call("LandingPage.callFromMacro");
</startupAction>


<line>
<button>
icon=../../../macros/LoadImage.png
arg=<macro>
call("ImageLoader.callFromMacro");
</macro>

<separator>

<button>
icon=../../../macros/SaveMarkup.png
arg=<macro>
call("ImageSaver.callFromMacro");
</macro>

<separator>

<button>
icon=../../../macros/RunAlignment.png
arg=<macro>
call("AlignRunner.callFromMacro");
</macro>

</line>
<separator>
<line>

<button>
icon=../../../macros/About.png
arg=<macro>
call("AboutPage.callFromMacro");
</macro>

<separator>

<button>
icon=../../../macros/Settings.png
arg=<hide>

<separator>

<button>
icon=../../../macros/Exit.png
arg=<macro>
run("Quit");
</macro>

</line>

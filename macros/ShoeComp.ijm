<main>
<disableAltClose>
<disableCtrlAltEdit>
<hideMenus>
<noGrid>

<startupAction>
eval("js", "if(IJ.getInstance().isVisible()) IJ.getInstance().setVisible(false);");
</startupAction>

<line>
<button>
icon=../../../macros/LoadImage.png
arg=<macro>
call("Image_Loader.callFromMacro");
</macro>

<separator>

<button>
icon=../../../macros/SaveMarkup.png
arg=<macro>
call("Image_Saver.callFromMacro");
</macro>

<separator>

<button>
icon=../../../macros/RunAlignment.png
arg=<macro>
call("Align_Runner.callFromMacro");
</macro>

</line>
<separator>
<line>

<button>
icon=../../../macros/About.png
arg=<macro>
call("About_Page.callFromMacro");
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

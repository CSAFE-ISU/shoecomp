<main>
<disableAltClose>
<disableCtrlAltEdit>
<hideMenus>

<startupAction>
eval("js", "if(IJ.getInstance().isVisible()) IJ.getInstance().setVisible(false);");
</startupAction>

<line>
<button>
label=<html><h1 style="font-weight: normal">Load Image</h1></html>
arg=<macro>
call("Image_Loader.callFromMacro");
</macro>

<separator>

<button>
label=<html><h1 style="font-weight: normal">Save Markup</h1></html>
arg=<macro>
call("Image_Saver.callFromMacro");
</macro>

<separator>

<button>
label=<html><h1 style="font-weight: normal">Run Alignment</h1></html>
arg=<macro>
call("Align_Runner.callFromMacro");
</macro>

</line>
<separator>
<line>

<button>
label=<html><h1 style="font-weight: normal">About</h1></html>
arg=<macro>
call("About_Page.callFromMacro");
</macro>

<separator>

<button>
label=<html><h1 style="font-weight: normal">Debug</h1></html>
arg=<hide>

<separator>

<button>
label=<html><h1 style="font-weight: normal">Exit</h1></html>
arg=<macro>
run("Quit");
</macro>

</line>

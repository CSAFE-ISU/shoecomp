run("Action Bar","/macros/ShoeComp.ijm");
selectWindow("ShoeComp");
exit();

<main>
<hide>
<disableAltClose>
<disableCtrlAltEdit>
<hideMenus>

<line>
<button>
label=Load Image
arg=<macro>
call("Image_Loader.callFromMacro");
</macro>

<separator>

<button>
label=Save Markup
arg=<macro>
call("Image_Saver.callFromMacro");
</macro>

<separator>

<button>
label=Run Alignment
arg=<macro>
call("Align_Runner.callFromMacro");
</macro>

</line>
<separator>
<line>

<button>
label=About
arg=<macro>
call("About_Page.callFromMacro");
</macro>

<separator>

<button>
label=Debug
arg=<hide>

<separator>

<button>
label=Exit
arg=<macro>
run("Quit");
</macro>

</line>

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
print("Hello");
</macro>

<separator>

<button>
label=Save Markup
arg=<macro>
print("Hello");
</macro>

<separator>

<button>
label=Run Alignment
arg=<macro>
print("Hello");
</macro>

</line>
<separator>
<line>

<button>
label=About
arg=<macro>
print("Hello");
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

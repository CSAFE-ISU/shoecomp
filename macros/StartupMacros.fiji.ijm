// "StartupMacros"

// The macro named "AutoRun" runs when ImageJ starts.

macro "AutoRun" {
    // hide ImageJ window at startup
    eval("js", "IJ.getInstance().setLocation(0, 0)");
    eval("js", "if(IJ.getInstance().isVisible()) IJ.getInstance().setVisible(false);");
    call("LandingPage.callFromMacro");
    // run("Action Bar","/macros/ShoeComp.ijm");
	// run("Install...", "/macros/ShoeComp_Shortcuts.ijm");
}

// "StartupMacros"

// The macro named "AutoRun" runs when ImageJ starts.

macro "AutoRun" {
    // hide ImageJ window at startup
    eval("js", "IJ.getInstance().setLocation(0, 0)");
    eval("js", "IJ.getInstance().setVisible(false)");
    runMacro("ShoeComp");
}

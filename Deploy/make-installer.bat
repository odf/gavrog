set IZPACK=%HOME%\My Documents\Software\IzPack
set GAVROG_MAIN=%HOME%\My Documents\workspace-3.2.2\Gavrog Main
set GAVROG_3DT=%HOME%\My Documents\workspace-3.2.2\Gavrog-3dt

jar cmf "%GAVROG_MAIN%\SYSTRE.MF" Systre.jar -C "%GAVROG_MAIN%\bin" org -C "%GAVROG_MAIN%\bin" buoy
jar cf 3dt-Main.jar -C "%GAVROG_3DT%\bin" org

call "%IZPACK%\bin\compile" install.xml

del Systre.jar 3dt-Main.jar

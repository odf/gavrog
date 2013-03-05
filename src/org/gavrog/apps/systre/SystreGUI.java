/*
Copyright 2013 Olaf Delgado-Friedrichs

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.gavrog.apps.systre;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.gavrog.box.collections.Pair;
import org.gavrog.box.gui.ExtensionFilter;
import org.gavrog.box.gui.Invoke;
import org.gavrog.box.gui.OptionCheckBox;
import org.gavrog.box.gui.OptionInputBox;
import org.gavrog.box.simple.DataFormatException;
import org.gavrog.box.simple.Misc;
import org.gavrog.box.simple.TaskController;
import org.gavrog.box.simple.TaskStoppedException;
import org.gavrog.joss.geometry.SpaceGroupCatalogue;
import org.gavrog.joss.pgraphs.embed.ProcessedNet;
import org.gavrog.joss.pgraphs.io.Archive;
import org.gavrog.joss.pgraphs.io.Net;
import org.gavrog.joss.pgraphs.io.Output;

import buoy.event.CommandEvent;
import buoy.event.DocumentLinkEvent;
import buoy.event.WindowClosingEvent;
import buoy.event.WindowResizedEvent;
import buoy.widget.BButton;
import buoy.widget.BDialog;
import buoy.widget.BDocumentViewer;
import buoy.widget.BFileChooser;
import buoy.widget.BFrame;
import buoy.widget.BLabel;
import buoy.widget.BOutline;
import buoy.widget.BScrollBar;
import buoy.widget.BScrollPane;
import buoy.widget.BSeparator;
import buoy.widget.BStandardDialog;
import buoy.widget.BTextArea;
import buoy.widget.BorderContainer;
import buoy.widget.ColumnContainer;
import buoy.widget.GridContainer;
import buoy.widget.LayoutInfo;

/**
 * A simple GUI for Gavrog Systre.
 */
public class SystreGUI extends BFrame {
	final static String mainLabel = ""
			+ "<html><h1><font color=#202060>Gavrog Systre</font></h1>"
			+ "<font color=#202060>Version " + Version.full + "<br>"
			+ "by Olaf Delgado-Friedrichs, 2001-2013</font></html>";
	
	// --- some constants used in the GUI
    final private static Color textColor = new Color(255, 250, 240);
	final private static Color buttonColor = new Color(224, 224, 240);
	final private static Insets defaultInsets = new Insets(5, 5, 5, 5);

	// --- file choosers
    final private BFileChooser inFileChooser =
            new BFileChooser(BFileChooser.OPEN_FILE, "Open data file");
    final private BFileChooser outFileChooser =
            new BFileChooser(BFileChooser.SAVE_FILE, "Save output");

    // --- GUI elements that need to be accessed by more than one method
    final private BTextArea output;
    final private BScrollBar vscroll;
    final private BButton openButton;
    final private BButton nextButton;
    final private BButton saveButton;
    final private BButton optionsButton;
    final private BLabel statusBar;
    
    // --- the object doing the actual processing
    private final SystreCmdline systre = new SystreCmdline();
    
    // --- fields to store some temporary information
    private Iterator<Net> netsToProcess = null;
	private Exception inputException = null;
	private String strippedFileName;
    private String fullFileName;
    private StringBuffer currentTranscript = new StringBuffer();
    private String lastFinishedTranscript = null;
    private List<Pair<ProcessedNet, String>> bufferedNets =
            new LinkedList<Pair<ProcessedNet, String>>();
    private int count;
	private TaskController taskController = null;
    
    // --- options
    private boolean singleWrite = false;
    private boolean readArchivesAsInput = false;
    private boolean nonStopMode = false;

    // --- configuration file path
    private String configFileName = "";
    
    /**
     * Constructs an instance.
     */
    public SystreGUI() {
		super("Systre " + Version.full);

		configFileName = System.getProperty("user.home") + "/.systrerc";
		systre.loadOptions(configFileName);
		
        final String archivePath = System.getProperty("user.home") + "/.systre";
        for (final String filename: userDefinedArchives(archivePath)) {
            systre.processArchive(filename);
        }
        
		final BorderContainer main = new BorderContainer();
		main.setDefaultLayout(
		        new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH, null, null));
		main.setBackground(textColor);

		final BorderContainer top = new BorderContainer();
		top.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.NONE,
				defaultInsets, null));
		top.setBackground(null);
		final BLabel label = new BLabel(mainLabel);
		top.add(label, BorderContainer.NORTH);

        final GridContainer buttonBar = new GridContainer(5, 1);
        buttonBar.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER,
                LayoutInfo.HORIZONTAL, null, null));
        buttonBar.add(openButton = makeButton("Open...", this, "doOpen"), 0, 0);
        buttonBar.add(nextButton = makeButton("Next", this, "doNext"), 1, 0);
        buttonBar.add(saveButton =
                makeButton("Save as...", this, "doSave"), 2, 0);
        buttonBar.add(optionsButton =
                makeButton("Options...", this, "doOptions"), 3, 0);
        buttonBar.add(makeButton("Help", this, "doHelp"), 4, 0);
        top.add(buttonBar, BorderContainer.CENTER,
                new LayoutInfo(LayoutInfo.CENTER,
				LayoutInfo.HORIZONTAL, null, null));

        statusBar = new BLabel();
        final BOutline outline =
                BOutline.createLineBorder(statusBar, Color.BLACK, 2);
        outline.setBackground(Color.WHITE);
        top.add(outline, BorderContainer.SOUTH, new LayoutInfo(LayoutInfo.WEST,
                LayoutInfo.HORIZONTAL, null, null));
        
        main.add(top, BorderContainer.NORTH);
		
		output = new BTextArea(20, 40);
		output.setBackground(null);
		final BScrollPane scrollPane = new BScrollPane(output,
				BScrollPane.SCROLLBAR_ALWAYS, BScrollPane.SCROLLBAR_ALWAYS);
		scrollPane.setForceHeight(true);
		scrollPane.setForceWidth(true);
		this.vscroll = scrollPane.getVerticalScrollBar();
		scrollPane.setBackground(null);
		main.add(scrollPane, BorderContainer.CENTER);
		
		final BButton cancelButton = makeButton("Cancel", this, "doCancel");
		final BButton exitButton = makeButton("Exit", this, "doQuit");
		final BorderContainer bottom = new BorderContainer();
        bottom.setDefaultLayout(
                new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.NONE,
                        defaultInsets, null));
		bottom.setBackground(null);
		bottom.add(cancelButton, BorderContainer.WEST);
		bottom.add(exitButton, BorderContainer.EAST);

        main.add(bottom, BorderContainer.SOUTH,
                new LayoutInfo(LayoutInfo.CENTER,
                        LayoutInfo.HORIZONTAL, null, null));
        
        nextButton.setEnabled(false);
        saveButton.setEnabled(false);
        
        final JFileChooser inchsr = inFileChooser.getComponent();
        inchsr.addChoosableFileFilter(new ExtensionFilter(new String[] {
        		"ds", "tgs" }, "Delaney-Dress Symbol Files"));
        inchsr.addChoosableFileFilter(
                new ExtensionFilter("arc", "Systre Archives"));
        inchsr.addChoosableFileFilter(
                new ExtensionFilter(new String[] {"cgd", "pgr" },
        		"Systre Input Files"));
        final JFileChooser outchsr = outFileChooser.getComponent();
        outchsr.addChoosableFileFilter(
                new ExtensionFilter("arc", "Systre Archive Files"));
        outchsr.addChoosableFileFilter(
                new ExtensionFilter("cgd", "Embedded Nets"));
        outchsr.addChoosableFileFilter(
                new ExtensionFilter("pgr", "Abstract Topologies"));
        outchsr.addChoosableFileFilter(
                new ExtensionFilter("out", "Systre Transcripts"));
        
        systre.addEventLink(String.class, this, "status");
        statusBar.setText("...");

        setContent(main);
        pack();
		final JFrame jf = getComponent();
		jf.setSize(700, 600);
		jf.validate();

        addEventLink(WindowClosingEvent.class, this, "doQuit");
        addEventLink(WindowResizedEvent.class, this, "resizeMessage");

        captureOutput();
        
		setVisible(true);
        
        status("Ready to go!");
    }
    
    private List<String> userDefinedArchives(final String path)
    {
        final List<String> archives = new LinkedList<String>();
        
        for (File f: new File(path).listFiles())
            archives.add(path + "/" + f.getName());
        
        return archives;
    }
    
    public void resizeMessage() {
    	final Dimension size = getComponent().getSize();
    	status("Window resized to " + size.width + "x" + size.height + ".");
    }
    
    public void status(final String text) {
		Invoke.andWait(new Runnable() {
			public void run() {
				statusBar.setText("<html><font color=\"green\">&nbsp;" + text
						 + "</font></html>");
			}
		});
	}
    
    private BButton makeButton(final String label, final Object target,
            final String method) {
    	final BButton button = new BButton(label);
    	button.setBackground(buttonColor);
        button.addEventLink(CommandEvent.class, target, method);
    	return button;
    }
    
    private void captureOutput() {
        final OutputStream stream = new OutputStream() {
            private StringBuffer buffer = new StringBuffer(128);

            public void write(int b) throws IOException {
                final char c = (char) b;
                buffer.append(c);
                if (c == '\n' || buffer.length() > 1023) {
                    flush();
                }
            }
            
            public void flush() {
                output.append(buffer.toString());
                currentTranscript.append(buffer);
                buffer.delete(0, buffer.length());
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        vscroll.setValue(vscroll.getMaximum());
                    }
                });
            }
        };

        this.systre.setOutStream(new PrintStream(stream));
    }
    
    public void doOpen() {
        final boolean success = this.inFileChooser.showDialog(this);
        if (success) {
        	this.netsToProcess = null;
            final String filename =
                    this.inFileChooser.getSelectedFile().getName();
            final File dir = this.inFileChooser.getDirectory();
            final String path = new File(dir, filename).getAbsolutePath();
            this.output.setText("");
            disableMainButtons();
            
            if (!this.readArchivesAsInput && filename.endsWith(".arc")) {
                systre.processArchive(path);
                enableMainButtons();
            } else {
                openFile(path);
                doNext();
            }
        }
    }
    
    public void doSave() {
        final String name = this.strippedFileName;
        this.outFileChooser.setSelectedFile(new File(name + ".out"));
        final boolean success = this.outFileChooser.showDialog(this);
        if (success) {
            String filename = this.outFileChooser.getSelectedFile().getName();
            final int n = filename.lastIndexOf('.');
            final String ext = filename.substring(n+1);
            final String filetype;
        	final ExtensionFilter filter = (ExtensionFilter) outFileChooser
					.getFileFilter();
			if (filter.accept(new File("x.cgd"))) {
				filetype = "cgd";
			} else if (filter.accept(new File("x.pgr"))) {
				filetype = "pgr";
			} else if (filter.accept(new File("x.arc"))) {
				filetype = "arc";
			} else {
				filetype = "out";
			}
            if (!ext.equals(filetype)) {
            	filename = filename + "." + filetype;
            }
            
            final File dir = this.outFileChooser.getDirectory();
            final File file = new File(dir, filename);
            final boolean append;
            if (file.exists()) {
                final int choice = new BStandardDialog("Systre - File exists",
						"File \"" + file + "\" exists. Overwrite?",
						BStandardDialog.QUESTION).showOptionDialog(this,
						new String[] { "Overwrite", "Append", "Cancel" },
						"Cancel");
                if (choice > 1) {
                    return;
                } else {
                    append = choice == 1;
                }
            } else {
                append = false;
            }
            disableMainButtons();
            
            new Thread(new Runnable() {
                public void run() {
                    try {
                        final BufferedWriter writer = new BufferedWriter(
                                new FileWriter(file, append));
                        if (singleWrite) {
							writeStructure(filetype, writer,
							        systre.getLastStructure(),
									lastFinishedTranscript);
						} else {
							for (final Pair<ProcessedNet, String> item:
							    bufferedNets)
							{
								final ProcessedNet net = item.getFirst();
								final String transcript = item.getSecond();
								writeStructure(
								        filetype, writer, net, transcript);
							}
							// -- save output from a possible frozen computation
							writeStructure(filetype, writer, null,
									currentTranscript.toString());
						}
						writer.flush();
						writer.close();
                    } catch (IOException ex) {
                        reportException(null, "FILE", "I/O error writing to "
                                + file, false);
                    } catch (Exception ex) {
                        reportException(ex, "INTERNAL",
                                "Unexpected exception while writing to "
                                        + file, true);
                    } finally {
                        enableMainButtons();
                    }
                }
            }).start();
        }
    }
    
    private void writeStructure(
            final String extension,
            final BufferedWriter writer,
			final ProcessedNet net,
			final String transcript) throws IOException
	{
        if ("out".equals(extension)) {
            final String lineSeparator = System.getProperty("line.separator");
            // --- write the full transcript
            writer.write(transcript.replaceAll(lineSeparator, "\n"));
            writer.write("\n");
        } else if (net != null) {
            if ("arc".equals(extension)) {
            	// --- write archive entry
				final String txt = new Archive.Entry(net.getGraph(),
				        net.getName()).toString();
				writer.write(txt);
				writer.write("\n");
            } else if ("cgd".equals(extension)) {
            	// --- write embedding structure with full symmetry
                net.writeEmbedding(
                        writer, true, systre.getOutputFullCell(), "");
            } else if ("pgr".equals(extension)) {
            	// --- write abstract, unembedded periodic graph
                Output.writePGR(
                        writer, net.getGraph().canonical(), net.getName());
				writer.write("\n");
            }
        }
    }
    
    public void doOptions() {
		final BDialog dialog = new BDialog(this, "Systre - Options", true);
		final ColumnContainer column = new ColumnContainer();
		column.setDefaultLayout(
		        new LayoutInfo(LayoutInfo.WEST, LayoutInfo.HORIZONTAL,
		                defaultInsets, null));
        column.setBackground(textColor);
        try {
			column.add(new OptionCheckBox(
			        "Process whole files without stopping",
					this, "nonStopMode"));
			column.add(new OptionCheckBox("Use Builtin Archive", this.systre,
					"useBuiltinArchive"));
			column.add(new OptionCheckBox(
			        "Process '.arc' files like normal input",
					this, "readArchivesAsInput"));
			column.add(new BSeparator());
			column.add(new OptionCheckBox("Prefer Second Origin On Input",
					SpaceGroupCatalogue.class, "preferSecondOrigin"));
			column.add(new OptionCheckBox("Prefer Hexagonal Setting On Input",
					SpaceGroupCatalogue.class, "preferHexagonal"));
            column.add(new BSeparator());
            column.add(new OptionCheckBox("Compute Wells point symbols",
                    this.systre, "computePointSymbols"));
			column.add(new BSeparator());
			column.add(new OptionCheckBox("Compute an Embedding", this.systre,
					"computeEmbedding"));
            column.add(new OptionCheckBox("Start from given Embedding",
                    this.systre, "useOriginalEmbedding"));
			column.add(new OptionCheckBox("Relax Node Positions", this.systre,
					"relaxPositions"));
			column.add(new OptionInputBox("Importance Of Equal Edge Lengths",
					this.systre, "relaxPasses"));
			column.add(new OptionInputBox("Relaxation Step Limit",
					this.systre, "relaxSteps"));
			column.add(new BSeparator());
			column.add(new OptionCheckBox("Output Complete Unit Cell Contents",
					this.systre, "outputFullCell"));
			column.add(new OptionCheckBox("Save only last net finished", this,
					"singleWrite"));
			column.add(new BSeparator());
		} catch (final Exception ex) {
			reportException(ex, "FATAL", "serious internal problem", true);
			return;
		}
        
        final BorderContainer bottom = new BorderContainer();
        bottom.setDefaultLayout(
                new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.NONE,
                        defaultInsets, null));
        bottom.setBackground(null);
        bottom.add(makeButton("Ok", dialog, "dispose"), BorderContainer.WEST);
        bottom.add(makeButton("Save", this, "doSaveOptions"),
                BorderContainer.EAST);

        column.add(bottom, new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.NONE,
                defaultInsets, null));

		dialog.setContent(column);

		dialog.addEventLink(WindowClosingEvent.class, dialog, "dispose");

		dialog.pack();
		dialog.setVisible(true);
	}
    
    public void doNext() {
        disableMainButtons();
        new Thread(new Runnable() {
            public void run() {
                nextNet();
                enableMainButtons();
            }
        }).start();
    }
    
    private boolean moreNets() {
    	status("Reading next net...");
    	boolean more;
    	try {
    		more = this.netsToProcess != null && this.netsToProcess.hasNext();
    	} catch (final Exception ex) {
    		this.inputException = ex;
    		more = true;
    	}
    	if (more) {
    		status("Press \"Next\" to process next net.");
    	} else {
    		status("End of file reached.");
    	}
    	return more;
    }
    
    private Net getNextNet() throws Exception {
    	if (this.inputException != null) {
    		final Exception ex = this.inputException;
    		this.inputException = null;
    		throw ex;
    	} else {
    		final Net result = this.netsToProcess.next();
    		if (!result.isOk()) {
    			throw result.getErrors().next();
    		}
            this.taskController.bailOutIfCancelled();
            return result;
    	}
    }
    
    public void nextNet() {
        final String stopped = "Execution stopped for this structure";
    	this.taskController = TaskController.getInstance();
    	
        if (!moreNets()) {
            finishFile();
            return;
        }
        
        final PrintStream out = this.systre.getOutStream();
        Net G = null;
        Exception problem = null;
        this.currentTranscript.delete(0, this.currentTranscript.length());
        status("Reading the next net...");
        boolean cancelled = false;

        final class BailOut extends Throwable {
            private static final long serialVersionUID = -7490696217023106703L;
        };
        final class Cancel  extends Throwable {
            private static final long serialVersionUID = 5411581838341297732L;
        };
        
        try {
            ++this.count;
            // --- some blank lines as separators
            out.println();
            if (this.count > 1) {
                out.println();
                out.println();
            }
            // --- read the next net
            try {
            	G = getNextNet();
            } catch (TaskStoppedException ex) {
                reportException(new SystreException(
                        SystreException.CANCELLED, stopped), "CANCELLED",
                        null, false);
                throw new Cancel();
            } catch (DataFormatException ex) {
                problem = ex;
            } catch (Exception ex) {
                reportException(ex, "INTERNAL", "Unexpected exception", true);
                throw new BailOut();
            }
            if (G == null) {
                reportException(problem, "INPUT", null, false);
                throw new BailOut();
            }
            final String archiveName;
            final String displayName;
            if (G.getName() == null) {
                archiveName = this.strippedFileName + "-#" + this.count;
                displayName = "";
            } else {
                archiveName = G.getName();
                displayName = " - \"" + G.getName() + "\"";
            }
            out.println("Structure #" + this.count + displayName + ".");
            out.println();
  
            if (G.getWarnings().hasNext())
            {
            	out.println("==================================================");
            	for (Iterator<String> iter = G.getWarnings(); iter.hasNext();)
            		out.println("!!! WARNING (INPUT) - " + iter.next());
                out.println("==================================================");
                out.println();
            }
                        
            boolean success = false;
            if (problem != null) {
                reportException(problem, "INPUT", null, false);
            } else {
                try {
                    this.systre.processGraph(G, archiveName, true);
                    success = true;
                } catch (TaskStoppedException ex) {
                    reportException(new SystreException(
                            SystreException.CANCELLED, stopped), "CANCELLED",
                            null, false);
                    cancelled = true;
                } catch (SystreException ex) {
                    reportException(ex, ex.getType().toString(), null, false);
                } catch (Exception ex) {
                    reportException(
                            ex, "INTERNAL", "Unexpected exception", true);
                } catch (Error ex) {
                	reportException(ex, "EXECUTION", "Runtime problem", true);
                }
            }
            out.println();
            out.println("Finished structure #" + this.count + displayName
                    + ".");
            this.lastFinishedTranscript = this.currentTranscript.toString();
            final ProcessedNet tmp;
            if (success) {
                tmp = this.systre.getLastStructure();
            }
            else {
                tmp = null;
            }
            this.bufferedNets.add(new Pair<ProcessedNet, String>(
                    tmp, this.lastFinishedTranscript));
        } catch (BailOut ex) {
            out.println();
            out.println("Skipping structure #" + this.count
                    + " due to error in input.");
        } catch (Cancel ex) {
        	cancelled = true;
        }
        if (!moreNets()) {
            finishFile();
        } else if (getNonStopMode() && cancelled) {
        	out.println();
        	out.println("'Cancel' pressed in non-stop mode. " +
        				"Skipping rest of file.");
            finishFile();
        } else if (getNonStopMode()) {
        	nextNet();
        }
        
        this.taskController = null;
    }
    
    private boolean openFile(final String filePath) {
        final PrintStream out = this.systre.getOutStream();

        this.netsToProcess = null;
        this.count = 0;
        
        this.fullFileName = filePath;
        this.strippedFileName =
                new File(filePath).getName().replaceFirst("\\..*$", "");
        this.bufferedNets.clear();
        this.inputException = null;

        try {
            this.netsToProcess = Net.iterator(filePath);
        } catch (FileNotFoundException ex) {
            reportException(ex, "FILE", null, false);
            return false;
        } catch (Net.IllegalFileNameException ex) {
            reportException(ex, "FILE", null, false);
            return false;
        } catch (Exception ex) {
            reportException(ex, "INTERNAL", "Unexpected exception", true);
        } catch (Error ex) {
            reportException(ex, "EXECUTION", "Runtime problem", true);
        }
        if (this.netsToProcess == null) {
            return false;
        } else {
            out.println("Data file \"" + filePath + "\".");
            return true;
        }
	}

    private void finishFile() {
        final PrintStream out = this.systre.getOutStream();
        status("End of file reached!");
        out.println();
        out.println("Finished data file \"" + this.fullFileName + "\".");
        this.netsToProcess = null;
        this.inputException = null;
    }

    private void reportException(final Throwable ex, final String type,
			final String msg, final boolean details) {
		final PrintStream out = systre.getOutStream();
		out.println();
		if (details) {
			out.println("==================================================");
		}
		final boolean cancelled = ex instanceof SystreException
				&& ((SystreException) ex).getType().equals(
				        SystreException.CANCELLED);
		final String text;
		if (cancelled) {
			text = "CANCELLING";
		} else {
			text = "ERROR (" + type + ") - " + (msg == null ? "" : msg);
		}
        out.print("!!! " + text);
        if (ex != null) {
            if (details) {
                out.println();
                out.print(Misc.stackTrace(ex));
                out.println("==================================================");
            } else {
                out.println((ex != null ? " - " + ex.getMessage() : "") + ".");
            }
        }
        
        if (!getNonStopMode()) {
			Invoke.andWait(new Runnable() {
				public void run() {
					final String title = "Systre: " + type + " ERROR";
					final String msg = text
							+ (ex != null ? " - " + ex.getMessage() : "") + ".";
					final BStandardDialog dialog = new BStandardDialog(title,
							msg, BStandardDialog.ERROR);
					dialog.showMessageDialog(SystreGUI.this);
				}
			});
		}
	}
    
    private void disableMainButtons() {
        Invoke.andWait(new Runnable() {
            public void run() {
                openButton.setEnabled(false);
                nextButton.setEnabled(false);
                saveButton.setEnabled(false);
                optionsButton.setEnabled(false);
            }
        });
    }

    private void enableMainButtons() {
        Invoke.later(new Runnable() {
            public void run() {
                openButton.setEnabled(true);
                if (moreNets()) {
                	nextButton.setEnabled(true);
                }
                saveButton.setEnabled(true);
                optionsButton.setEnabled(true);
            }
        });
    }
    
    public void doHelp() {
		final BFrame frame = new BFrame("Systre - Help");
		final BorderContainer content = new BorderContainer();
		content.setDefaultLayout(
		        new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH,
                defaultInsets, null));
        content.setBackground(textColor);
		final BDocumentViewer viewer = new BDocumentViewer() {
			public void processLinkEvent(final DocumentLinkEvent event)
					throws IOException {
				if (event.getURL() == null) {
					final String ref = event.getDescription();
					if (ref.startsWith("#")) {
						((JEditorPane) component)
						.scrollToReference(ref.substring(1));
					}
				}
			}
		};
		viewer.setBackground(null);
		final BScrollPane scrollPane = new BScrollPane(viewer,
				BScrollPane.SCROLLBAR_AS_NEEDED, BScrollPane.SCROLLBAR_ALWAYS);
		scrollPane.setBackground(null);
		scrollPane.setForceHeight(true);
		scrollPane.setForceWidth(true);
		content.add(scrollPane, BorderContainer.CENTER);
		frame.setContent(content);

		frame.addEventLink(WindowClosingEvent.class, frame, "dispose");
		viewer.addEventLink(
		        DocumentLinkEvent.class, viewer, "processLinkEvent");
		
        new Thread(new Runnable() {
			public void run() {
				final Package pkg = this.getClass().getPackage();
				final String packagePath = pkg.getName().replaceAll("\\.", "/");

				final String textPath = packagePath + "/Systre-Help.html";
				final InputStream textStream = ClassLoader
						.getSystemResourceAsStream(textPath);
				final BufferedReader textReader = new BufferedReader(
						new InputStreamReader(textStream));
				final StringBuffer buf = new StringBuffer(10000);
				while (true) {
					final String line;
					try {
						line = textReader.readLine();
					} catch (final IOException ex) {
						break;
					}
					if (line == null) {
						break;
					}
					buf.append(line);
					buf.append("\n");
				}
				viewer.setDocument(buf.toString(), "text/html");
				scrollPane.getVerticalScrollBar().setValue(0);

				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						frame.pack();
						final JFrame jf = frame.getComponent();
						jf.setSize(700, 600);
						jf.validate();
						frame.setVisible(true);
					}
				});
			}
		}).start();
    }
    
    public void doCancel() {
		this.systre.cancel();
        saveButton.setEnabled(true);
    	if (this.taskController != null) {
    		this.taskController.cancel();
    	}
    }
    
    public void doSaveOptions() {
        systre.saveOptions(configFileName);
    }
    
    public void doQuit() {
        System.exit(0);
    }
    
	public boolean getSingleWrite() {
		return singleWrite;
	}

	public void setSingleWrite(boolean singleWrite) {
		this.singleWrite = singleWrite;
	}
	
	public boolean getReadArchivesAsInput() {
		return readArchivesAsInput;
	}

	public void setReadArchivesAsInput(boolean readArchivesAsInput) {
		this.readArchivesAsInput = readArchivesAsInput;
	}

	public boolean getNonStopMode() {
		return nonStopMode;
	}

	public void setNonStopMode(boolean nonStopMode) {
		this.nonStopMode = nonStopMode;
	}

	public static void main(final String args[]) {
        new SystreGUI();
    }
}

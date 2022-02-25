# Actions in OImaging

This page lists all the actions that are available in OImaging. A one-line list, then a [detailed list](#detailed-list-of-actions). There is also explanations about what is an *action* in the third section of the present document.

Anytime you plan to add, remove or modify a functionality of OImaging, you can use this list to see if your update fits well and preserves consistency.

## List of actions

Non-strictly grouped by theme:

### File loading from disk

- [`Load OIFitsFile`](#load-oifitsfile): load an OIFits file from disk and fill the input form with it
- [`Load FitsImageFile`](#load-fitsimagefile): load a Fits file from disk and add call `Add Image` for each image
- [`Load Result`](#load-result): load an OIFits file and calls action `Add Result` [DevMode restricted]

### File saving to disk

- [`Save OIFitsFile`](#save-oifitsfile): save an OIFits file (from a result or from the input form) to the disc memory
- [`Save FitsImageFile`](#save-fitsimagefile): save an image (from input form or from result) in a Fits file to the disc memory

### SAMP communication

- `Register SAMP`: register to the *SAMP hub*
- `SAMP Status:`: display *SAMP hub* status
- `Send OIFitsFile`: send an OIFits file by *SAMP*
- `Send FitsImageFile`: send a Fits file by *SAMP*
- `Receive OIFitsFile`: receive an OIFits file by *SAMP* and call `Load OIFitsFile`
- `Receive FitsImageFile`: receive a Fits file by *SAMP* and call `Load FitsImageFile`

### Images and Image Library

- [`Add Image`](#add-image): add an image to the library, respecting the no duplicates rule, and gives back an equivalent image from library [internal]
- [`Remove Image`](#remove-image): remove an image from the library
- [`Create Image`](#create-image): create a gaussian image and add call `Add Image`
- [`Modify Image`](#modify-image): modify *FOV* and *scale* of an image (from a result or from library) and call `Add Image`

See also the `Load FitsImageFile` action.

### Input form

- [`Start Run`](#start-run): start a *run* with the current input form. Call the asynchronous action `End Run`
- `Select Target`: select a target among the list of targets
- `Select Min Wave`: set effective minimum wavelength
- `Select Max Wave`: set effective maximum wavelength
- `Select Vis`: enable *visibilities*
- `Select software`: select a reconstruction software among the list of software
- `Software Help`: display basic help about selected software
- [`Select Init Image`](#select-init-image): select initial image among the image library
- [`Select Regul Image`](#select-regul-image): select regulation image among the image library
- `Set Manual Options`: set manual options for the software

This list is not exhaustive as the software parameters are somewhat dynamic. It always consists of some buttons, list of selections, text input...

### Results table

- [`Add Result`](#add-result): adds a result to the results table. [internal]
- `Select Result`: select a result (line) in the table
- `Select Several Results`: select several results (lines) in the table
- `Edit Cell`: edit the value of an editable cell
- `Drag & Drop Column`: move a column by drag & drop
- `Delete Result`: delete the selected result(s)
- `Compare Results`: display the *grid view* with several selected results
- `Select Columns`: select the columns to display (and set their order), and the columns to hide
- [`Run more iterations`](#run-more-iterations): call `Load OIFitsFile` with the selected result, then call `Start Run`, all this while staying on the *Results tab*
- [`Load result as input`](#load-result-as-input): call `Load OIFitsFile` with the selected result, and move to the *Input tab*
- [`Load result as input with last img`](#load-result-as-input-with-last-img): same as `Load result as input`, but the final image is used for the init image in the input form

### Viewer Panels

- `Display Image`: display the image view of the currently selected OIFitsFile
- `Display OIfits`: display the oifits data of the currently selected OIFitsFile
- `Display Parameters`: display parameters of the currently selected OIFitsFile
- `Display Execution Log`: display execution log of the currently selected OIFitsFile
- `Select Displayed Image`: select the image to display among the images of the currently selected OIFitsFile
- `Zoom Image`: zoom or dezoom the currently displayed image
- `Select LUT Table`: select *LUT table*
- `Select Color Scale`: select *color scale*
- `Display Keywords`: display the keywords associated to the currently displayed image
- `Ruler`: draw a ruler on the image and display computed length
- [`Set as init img`](#set-as-init-img): set the currently displayed image as init image in the input form, then move to *Input tab*

See also `Modify Image`.

### General

- [`End Run`](#end-result): adds the run's result to the table of results [internal]
- [`Reset`](#reset): clear all data and reset interface
- `Quit`: quit OImaging
- `Preferences`: display and edit OImaging preferences
- `Copyrights`: display copyrights
- `Feedback`: send feedback
- `Log Console`: display and tune log console
- `Release Notes`: display release notes
- `About`: display general information

## Detailed list of actions

Same list as above, with more informative content for each action.

### File loading from disk

#### Load OIFitsFile

- Goal: load an OIFits file from disk and fill the input form with it.
- Description:
  - Creates a Java `OIFitsFile` object from the data found in the file.
  - Sets the created `OIFitsFile` as the `OIFitsFile` associated to the input form.
  - Calls `Add Image` action for every image in the `OIFitsFile`.
  - Makes the input form widgets agree with the associated OIFits file
  - The output params are deleted.
  - The view is focused on the initial image of the input form.
- Called by: button "Load an OIFits file", menu button "File > Load OIFits file", action `Receive OIFitsFile`, action `Run More Iterations`, action `Load as Input`, action `Load as input with last img`.
- Notes:
  - Only the images pointed to by parameters `INIT_IMG` and `RGL_PRIO` are kept in the `OIFits` file.
  - May modify slightly the images. Indeed, the selected initial image in the `OIFitsFile` must also belong to the image library. However the image library refuses to contain two equivalents images. Thus, when loading the `OIFitsFile`, it there was already an equivalent image in the library, this latter image will replace the one in the `OIFitsFile`. This can also update the `INIT_IMG` input parameter, to reflect the potential update of `HDU_NAME`.
  - It is normal, if we select an initial image, and then execute the present action, to observe that our initial image has been set to `null`. Because this action makes the input form agree with the loaded `OIFitsFile`, if the latter has `INIT_IMG` parameter set to `null`, so must be  our initial image.

#### Load FitsImageFile

- Goal: load a Fits file from disk and add call `Add Image` for each image.
- Description:
  - Creates a Java `FitsImageFile` object from the data found in the file.
  - Adds every image from the `OIFits` file to the image library
  - Sets the first image as initial image in the input form
- Called by: button "Load a Fits Image file", menu button "File > Load Fits Image file", action `Receive FitsImageFile`.

#### Load Result

- Goal: load an OIFits file and add it to the result table.
- Description:
  - Rebuilds a complete `ServiceResult` object from the `OIFits` file
  - Calls the action `Add Result`.
- Called by: the *dev mode* loads all `OIFits` files from `~/.jmmc-/devmode/`. This action is unreachable from the interface.

### File saving to disk

#### Save OIFitsFile

- Goal: save an OIFits file (from input form or from result) to the disc memory
- Description:
  - If the view is focused on input form, the input form's associated `OIFitsFile` is the one saved. Else, if the view is focused on results, the `OIFits` file associated to the one selected result is saved. If several results or zero result are selected, this function refuses to apply.
- Called by: button "Save OIFits file", and menu button "File > Save OIFits file".

#### Save FitsImageFile

- Goal: save an image (from input form or from result) in a Fits file to the disc memory.
- Description:
  - Create a new FitsImageFile object.
  - Add to it the FitsImageHDU associated to the currently displayed image.
  - Save the FitsImageFile to a file.
- Called by: menu button "File > Save Fits Image file"

### Images and Image Library

#### Add Image

- Goal: add an image to the library, respecting the no duplicates rule, and gives back an equivalent image from library.
- Description:
  - The image is stored in a `FitsImageHDU` object
  - It is checked that there is no existing equivalent `FitsImageHDU` in the image library. If there is one, the action just gives back this latter one. If there is none, the action returns the new `FitsImageHDU`. Equivalence is defined by the `MATCHER` function in `FitsImageHDU`.
  - Unicity of `hduName` is enforced in the image library. A new added `FitsImageHDU` can then have its `hduName` renamed in place.
- Called by: actions `Load FitsImageFile`,`Load OIFitsFile`, `Create Image`, `Modify Image`.
- Notes:
  - `null` or `NULL_IMAGE_HDU` cannot be added to the image library
  - a `FitsImageHDU` with no images cannot be added to the image library
  - if you don't want your `FitsImageHDU` to have its `HDU_NAME` modified, you should use a copy of it
  - this action cannot be called directly by the GUI user, it can only be by means of other actions.

#### Remove Image

- Goal: remove an image from the library
- Called by: button "-" in input form.
- Notes: Unselect initial and regulation images that were using the removed image.

#### Create Image

- Goal: create a gaussian image and add call `Add Image`
- Description:
  - User can specify *FOV* in mas, *increments* in mas, *FWHM* in mas, `hduName`. *Image size* in number of pixels is the quotient of the division `FOV / increments`. *Increments* are then adjusted to respect better the equation `FOV = increments * imageSize`.
  - Creates image and calls `Add Image`.
  - Interface focus on the created image.
- Called by: button "Create Image", menu button "Processing > Create Image"
- Notes:
  - Created image is a square, the length of it side is an integer, even and strictly positive.

#### Modify Image

- Goal: modify *FOV* and *scale* of an image (from a result or from library) and call `Add Image`
- Description:
  - Modification is not in place: a new `FitsImageHDU` is created to store the modified image. The original `FitsImageHDU` and its image remain unmodified.
  - User can modify *FOV* in mas, *increments* in mas. *Image size* in number of pixels is the quotient of the division `FOV / increments`. *FOV* and *increments* are adjusted to respect better the equation `FOV = increments * imageSize`.
  - If initial and regulation image were pointing to the original `FitsImageHDU`, they are updated to point to the new modified one.
- Called by: button "Modify Image", menu button "Processing > Modify Image".
- Notes:
  - Created image is a square, the length of it side is even and strictly positive.
  - `hduName` is prefixed by "modified-".

### Input form

#### Start Run

- Goal: start a *run* with the current input form. Call the asynchronous action `End Run`
- Description:
  - Create the temporary file corresponding to the `OIFitsFile` associated to the input form.
  - Call the algorithm (local or remote) and supply it with the temporary file.
  - Give back the execution flow. This action is too slow to be synchronous. When the algorithm returns, it will call the action `End Result`.
- Called by: button "Run", menu button "Processing > Run", action `Run More Iterations`.
- Source code: `RunAction.actionPerformed`, `RunFitActionWorker.computeInBackground`.

#### Select Init Image

- Goal: to select initial image among the image library.
- Description:
  - Sets the `INIT_IMG` input param to the hdu name of the `FitsImageHDU` selected, in the `OIFitsFile` associated to the input form.
  - Adds the `FitsImageHDU` to the `OIFitsFile`.
  - Removes any `FitsImageHDU` from the `OIFitsFile` that is not targeted by the `INIT_IMG` keyword or `RGL_PRIO`.
- Called by: selection list of initial image in the input form, actions `Load OIFitsFile`, `Load FitsImageFile`, `Create Image`, `Modify Image`, `Remove Image`, `Load result as input`, `Load result as input with last img`, `Set as init img`.
- Notes:
  - If it was called from the selection, list, it sets the focus on the initial image.
  - There is a special "null" HDU named `"[No Image]"`. It can be selected but it is only valid if the algorithm selected supports missing initial image. This HDU sets the `INIT_IMG` param to an empty string.
- Source code: `IRModel.setSelectedInputImageHDU`, `SoftwareSettingsPanel.updateModel`, `IRModel.updateOifitsFileHDUs`.

#### Select Regul Image

- Goal: to select regulation image among the image library.
- Description:
  - Sets the `RGL_PRIO` input param to the hdu name of the `FitsImageHDU` selected, in the `OIFitsFile` associated to the input form.
  - Adds the `FitsImageHDU` to the `OIFitsFile`.
  - Removes any `FitsImageHDU` from the `OIFitsFile` that is not targeted by the `INIT_IMG` keyword or `RGL_PRIO`.
- Called by:
  - selection list of regulation image in the input form, actions `Create Image`, `Modify Image`, `Remove Image`, `Load OIFitsFile`.
- Notes:
  - If it was called from the selection, list, it sets the focus on the regulation image.
  - There is a special "null" HDU named `"[No Image]"`. It can be selected but it is only valid if the algorithm selected supports missing initial image. This HDU sets the `INIT_IMG` param to an empty string.
- Source code: `IRModel.setSelectedRglPrioImageHdu`, `SoftwareSettingsPanel.updateModel`, `IRModel.updateOifitsFileHDUs`.

### Results table

#### Add Result

- Goal: adds a result to the results table.
- Description:
  - Parses the result if not done yet
  - Adds the result to the table
  - Adds some eventually missing OImaging keywords
  - Calls `Add Image` for each image in the results
- Called by: actions `End Result`, `Load Result`. Unreachable directly by the GUI.
- Notes:
  - Updates the parameters `INIT_IMG` and `RGL_PRIO` of the result in case one of its image has been renamed when added to the image library.

#### Run more iterations

- Goal: call `Load OIFitsFile` with the selected result, then call `Start Run`, all this while staying on the *Results tab*
- Description:
  - Calls action `Load result as input with last img`.
  - Calls asynchronously `Start Run` action (by the `RUN` event).
  - In summary, this actions permits to "continue" the selected result for more iterations.
- Called by: button "Run more iterations" in the action panel in the results tab.
- Notes:
  - Automatic tab switch usually triggered by the modification of the input form is manually disabled to keep the GUI on the results tab.
  - Only works if there is exactly one selected valid result.

#### Load result as input

- Goal: call `Load OIFitsFile` with the selected result, and move to the *Input tab*.
- Description:
  - Copies the `OIFitsFile` associated to the selected result.
  - Calls `Load OIFitsFile` with this `OIFitsFile`.
  - Switchs to input form tab.
Called by: button "Load result as input"
- Notes:
  - Only works if there is exactly one selected valid result.

#### Load result as input with last img

  - Goal: same as `Load result as input`, but the final image is used for the init image in the input form.
  - Description:
    - Copies the `OIFitsFile` associated to the selected result.
    - Calls `Load OIFitsFile` with this `OIFitsFile`.
    - Calls `Select Init Image` with the result image found in the `OIFitsFile`.
    - Switchs to input form tab.
  Called by: button "Load result as input", action `Run more iterations`.
  - Notes:
    - Only works if there is exactly one selected valid result.

### Viewer Panels

#### Set as init img

- Goal: sets the currently displayed image as init image in the input form, then moves to *Input tab*
- Description:
  - Retrieves the currently displayed image, it depends on which tab is currently active: *input* or *results*.
  - Calls `Add Image` with the image.
  - Calls `Select as init img` with the image.
  - Switches to *input* tab.
  - Sets the focus on the initial image.
- Called by: *results* viewer panel button "Set as Init Img".
- Notes:
  - there is no button for this action in the *input* tab's viewer panel, as the selection list is already here to fulfill the goal.
- Source code: `SetAsInitImgAction`, `IRModel.setAsInitImg`.

### General

#### End Result

- Goal: adds the run's result to the table of results.
- Description:
  - Calls `Add Result` with the result.
  - Sets `running` state to `false`.
- Called by: Asynchronously by `Start Run`, it is triggered when the algorithm execution has completed.
- Source code: `RunFitActionWorker.refreshUI`.

#### Reset

- Goal: clears all data and resets interface.
- Description:
  - Sets manual option to `null`.
  - Sets focus to initial image.
  - Sets selected initial and regulation images to `null`.
  - Empties image library.
  - Empties results table.
  - Sets `running` state to false.
  - Sets a basic initial `OIFitsFile` associated to the input form.
- Called by: OImaging startup, menu button "File > New OI Image file".
- Source code: `IRModel.reset`, `NewAction`.

## What is an action ?

An action is a useful goal, a useful task that can be achieved by the software during its execution.

It consists of a summary goal, a list of its steps, ordered or not, a list of the callers of the action, additional notes, and related source code.

It can be low-level enough to be realized by a single function in the source code, but it can also span over several functions. It goes the same for the GUI : the action can be fully realized by a single click on a button, or it can imply several interactions with the user, for example such as a dialog with an input text field and a validation button.\
However an action should not be too high-level, as a limit example "Use OImaging" is the most high-level possible action, and it is too broad too be relevant. The high-level tasks that span over a large sequence of actions will be described in the form of "scenarios of usage". For example "Reconstruct an image" is one relevant scenario of usage that implies a sequence of actions in OImaging.

### Who call an action ?

- **User & graphical user interface**\
This is the most common source of action. Typically the user clicks on a button, for example the "Create Image" button. Almost every widget use leads to an OImaging action, for example drag & drop a column in the *results table* triggers an action.
- **End of an asynchronous action**\
Some actions take a long time, for example the `Start Run` action asks a lengthy computation of an algorithm. \
These actions are split in two parts: the *start* part and the *end* part. The split reflects better the behaviour of the program, and also reflects the fact that the user can trigger other actions between *start* and *end*.\
A good example of an end of an asynchronous action is the `End Run` action. It is triggered when the algorithm answers with a final reconstructed image that is going to be displayed in the interface.
- **SAMP & other JMMC softwares**\
*JMMC* softwares are able to communicate by the *SAMP* system. For example the user can trigger an action on another *JMMC* software that will send an image to OImaging by *SAMP*, this will trigger an OImaging action.
- **Composite actions**\
Some actions implies other actions. For example, the `End Run` action implies the `Add Result` action to add the result to the table, and the `Select Init Image` to set the new initial image in the input form.\
Some actions are not directly available from the graphical interface and are solely called by other actions, these are marked as "[internal]".
- **Automatic**\
Some actions are triggered more or else automatically by the OImaging software. For example OImaging tries to connect to the *SAMP* hub by the action *Register SAMP*.

\
<style>body { max-width: 1000px; } img { max-width: 100%; }</style>

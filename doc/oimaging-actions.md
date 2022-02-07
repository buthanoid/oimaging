This page lists all the actions that are available in OImaging. A one-line list, then a detailed list. There is also explanations about what is an *action* in the third part of the page.

# List of actions

Non-strictly grouped by theme:

## File loading

- [`Load OIFitsFile`](#load-oifitsfile): load an OIFits file and fill the input form with it
- [`Load FitsImageFile`](#load-fitsimagefile): load a Fits file and add call `Add Image` for each image
- [`Load Result`](#load-result): load an OIFits file and add it to the result table

## File saving

- [`Save OIFitsFile`](#save-oifitsfile): save an OIFits file (from a result or from the input form) to the disc memory
- [`Save FitsImageFile`](#save-fitsimagefile): save an image in a Fits file (image from library of from result) to the disc memory

## SAMP communication

- `Register SAMP`: register to the *SAMP hub*
- `SAMP Status:`: display *SAMP hub* status
- `Send OIFitsFile`: send an OIFits file containing mainly data by *SAMP*
- `Send FitsImageFile`: send a Fits file containing mainly images by *SAMP*
- `Receive OIFitsFile`: receive an OIFits file by *SAMP* and call `Load OIFitsFile`
- `Receive FitsImageFile`: receive a Fits file by *SAMP* and call `Load FitsImageFile`

## Images and Image Library

- [`Add Image`](#add-image): add an image to the library, respecting the no duplicates rule, and gives back an equivalent image from library
- [`Remove Image`](#remove-image): remove an image from the library
- [`Create Image`](#create-image): create a gaussian image and add call `Add Image`
- [`Modify Image`](#modify-image): modify *FOV* and *scale* of an image (from a result or from library) and call `Add Image`

See also the `Load FitsImageFile` action.

## Input form

- [`Start Run`](#start-run): start a *run* with the current input form. Call the asynchronous action `End Run`
- `Select Target`: select a target among the list of targets
- `Select Min Wave`: set effective minimum wavelength
- `Select Max Wave`: set effective maximum wavelength
- `Select Vis`: enable *visibilities*
- `Select software`: select a reconstruction software among the list of software
- `Software Help`: display basic help about selected software
- `Select Init Image`: select initial image among the image library
- `Select Regul Image`: select regulation image among the image library
- `Set Manual Options`: set manual options for the software

This list is not exhaustive as the software parameters are somewhat dynamic. It always consists of some buttons, list of selections, text input...

## Results table

- `Select Result`: select a result (line) in the table
- `Select Several Results`: select several results (lines) in the table
- `Edit Cell`: edit the value of an editable cell
- `Drag & Drop Column`: move a column by drag & drop
- `Delete Result`: delete the selected result(s)
- `Compare Results`: display the *grid view* with several selected results
- `Select Columns`: select the columns to display (and set their order), and the columns to hide
- `Run more iterations`: call `Load OIFitsFile` with the selected result, then call `Start Run`, all this while staying on the *Results tab*
- `Load as input`: call `Load OIFitsFile` with the selected result, and move to the *Input tab*
- `Load as input with last img`: same as `Load as input`, but the final image is used for the init image in the input form

## Viewer Panels

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
- `Set as init img`: set the currently displayed image as init image in the input form, then move to *Input tab*

See also `Modify Image`.

## General

- `End Run`: add the run's result to the table of results, and prepare the input form for the next run
- `Reset`: clear all data and reset interface
- `Quit`: quit OImaging
- `Preferences`: display and edit OImaging preferences
- `Copyrights`: display copyrights
- `Feedback`: send feedback
- `Log Console`: display and tune log console
- `Release Notes`: display release notes
- `About`: display general information

# Detailed list of actions

Same list as above, with more informative content for each action.

## File loading

### Load OIFitsFile

- Goal: load an OIFits file and fill the input form with it.
- Description:
  - It copies the `OIFits` file in the `OIFits` file associated to the input form.
  - It adds every image in the file to the image library.
  - It makes the input form widgets agree with the associated OIFits file
  - The initial image in the `OIFits` file is set as initial image in the input form.
  - The regulation image in the `OIFits` file is set as regulation image in the input form.
  - The view is focused on the initial image of the input form.
- Trigger: button "Load an OIFits file", menu button "File > Load OIFits file".
- Notes:
  - Only the `FitsImageHDu` pointed to by parameters `INIT_IMG` and `RGL_PRIO` are kept in the `OIFits` file.
  - It may modify the loaded `OIFits` file. This is because the image library does not accept equivalent-duplicates, and the selected initial image in the input form must belong to the image library, and the selected initial image in the input form must be the one present in the `OIFits` file. Thus, if we load an `OIFits` file with an initial image that has an existing equivalent in the image library, we change the image HDU in the OIFits file to the one that is already in the image library. We modify parameter `INIT_IMG` accordingly.
  - It is normal if we select an initial image, then execute this action, and observe that our initial image has been set to `null`. Because this action makes the input form agree with the loaded `OIFitsFile`, if the latter has `INIT_IMG` parameter set to `null`, so must our initial image be.

### Load FitsImageFile

- Goal: load an `OIFits` file and add call `Add Image` for each image.
- Description:
  - It adds every image from the `OIFits` file to the image library
  - It set the first image as initial image in the input form
- Trigger: button "Load a Fits Image file", menu button "File > Load Fits Image file".

### Load Result

- Goal: load an `OIFits` file and add it to the result table.
- Description:
  - Rebuild a complete `ServiceResult` object from the `OIFits` file
  - It calls the action `End Result`.
- Trigger: the *dev mode* loads all `OIFits` files from `~/.jmmc-/devmode/`. This action is unreachable from the interface.

## File saving

### Save OIFitsFile

- Goal: save an `OIFits` file (from a result or from the input form) to the disc memory
- Description:
  - If the view is focused on input form, the input form's associated `OIFits` file is the one saved. Else, if the view is focused on results, the `OIFits` file associated to the one selected result is saved. If several results are selected this function refuses to apply.
- Trigger: button "Save OIFits file", and menu button "File > Save OIFits file".

### Save FitsImageFile

- Goal: save an image in an OIFits file (image from library of from result) to the disc memory.
- Description:
  - Create a new FitsImageFile object.
  - Add to it the FitsImageHDU associated to the currently displayed image.
  - Save the FitsImageFile to a file.
- Trigger: menu button "File > Save Fits Image file"

## Images and Image Library

### Add Image

- Goal: add an image to the library, respecting no duplicates, and gives back an equivalent image from library
- Description:
  - The image is stored in a `FitsImageHDU` object
  - It is checked that there is no existing equivalent `FitsImageHDU` in the image library. If there is one, the action just gives back this latter one. If there is none, the action returns the new `FitsImageHDU`. Equivalence is defined by `checksum` equality.
  - Unicity of `hduName` is enforced in the image library. A new added `FitsImageHDU` can then have its `hduName` renamed in place.
- Trigger: actions `Load FitsImageFile`,`Load OIFitsFile`, `Create Image`, `Modify Image`, `End Result`.
- Notes:
  - `null` or `NULL_IMAGE_HDU` cannot be added to the image library
  - a `FitsImageHDU` with no images cannot be added to the image library

### Remove Image

- Goal: remove an image from the library
- Notes: Unselect initial and regulation images that were using the removed image.

### Create Image

- Goal: create a gaussian image and add call `Add Image`
- Description:
  - User can specify *FOV* in mas, *increments* in mas, *FWHM* in mas, `hduName`. *Image size* in number of pixels is the quotient of the division `FOV / increments`. *Increments* are then adjusted to respect better the equation `FOV = increments * imageSize`.
  - Interface focus on the created image.
- Trigger: button "Create Image", menu button "Processing > Create Image"
- Notes:
  - Created image is a square, the length of it side is even and strictly positive.

### Modify Image

- Goal: modify *FOV* and *scale* of an image (from a result or from library) and call `Add Image`
- Description:
  - Modification is not in place: a new `FitsImageHDU` is created to store the modified image. The original `FitsImageHDU` and its image remain unmodified.
  - User can modify *FOV* in mas, *increments* in mas. *Image size* in number of pixels is the quotient of the division `FOV / increments`. *FOV* and *increments* are adjusted to respect better the equation `FOV = increments * imageSize`.
  - If initial and regulation image were pointing to the unmodified `FitsImageHDU`, they are updated to point to the new modified one.
  - Interface focus on the modified image.
- Trigger: button "Modify Image", menu button "Processing > Modify Image".
- Notes:
  - Created image is a square, the length of it side is even and strictly positive.
  - `hduName` is prefixed by "modified-".

## Input form

### Start Run

- Goal: start a *run* with the current input form. Call the asynchronous action `End Run`
- Description:
  - Prepare the file corresponding to the Oifitsfile associated to the input form.
  - Call the correct script (local, remote...) with the input file
  - Give back the execution flow (the answer of the script will be handled by action `End Result`)
- Trigger: button "Run", menu button "Processing > Run".

# What is an action ?

An action is a useful goal, a useful task that can be achieved by the software during its execution.

It consists of a summary goal, a detailed scenario of its steps, with eventual branches, failures, pre and post conditions, the source that triggered or called it.

It can be low-level enough to be realized by a single function in the source code, but it can also span over several functions. It goes the same for the GUI : the action can be fully realized by a single click on a button, or it can imply several interactions with the user, for example such as a dialog with an input text field and a validation button.\
However an action should not be too high-level, as a limit example "Use OImaging" is the most high-level possible action, and it is too broad too be relevant. The high-level tasks that span over a large sequence of actions will be described in the form of "scenarios of usage". For example "Reconstruct an image" is one relevant scenario of usage that implies a sequence of actions in OImaging.

## Sources of actions

The source of the action is the entity that called the action.

- **User & graphical user interface**\
This is the most common source of action. Typically the user clicks on a button, for example the "Create Image" button. Almost every widget use leads to an OImaging action, for example drag & drop a column in the *results table* triggers an action.
- **End of an asynchronous action**\
Some actions take a long time, for example the `Start Run` action asks a computation on a distant server. \
These actions are split in two parts: the *start* part and the *end* part. The split reflects better the behaviour of the program, and also reflects the fact that the user can trigger other actions between *start* and *end*.\
A good example of an end of an asynchronous action is the `End Run` action. It is triggered when the server answers with a final reconstructed image that is going to be displayed in the interface.
- **SAMP & other JMMC softwares**\
*JMMC* softwares are able to communicate by the *SAMP* system. For example the user can trigger an action on another *JMMC* software that will send an image to OImaging by *SAMP*, this will trigger an OImaging action.
- **Composite actions**\
Some actions implies other actions. For example, the `End Run` action implies the `Add result` action to add the result to the table, and the `Select input image` to set the new initial image in the input form.\
Some actions are not directly available from the graphical interface and are solely called by other actions.
- **Automatic**\
Some actions are triggered more or else automatically by the OImaging software. For example OImaging tries to connect to the *SAMP* hub by the action *Register SAMP*.

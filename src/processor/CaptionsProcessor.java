package processor;

import java.io.File;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;

//import processor.InputHandler;
//import processor.Caption;

public class CaptionsProcessor {

	private static final String SPACE_DELINEATOR = " ";
	private static final String NEWLINE_DELINEATOR = "\n";
	
	private static final String CAPTION_BREAK_DELINEATOR = NEWLINE_DELINEATOR;  //exists to give the option of preserving newlines in caption content string
	private static final boolean ENABLE_CONSOLE_OUTPUT = true;
	
	private static final int FIRST_CHARACTER = 0;

	/**
	 * Returns a List<String> containing each line of a text file in a new entry.
	 * @param file the text file to be parsed
	 * @return the list of lines in type String
	 */
	public static List<String> fileToList(File file) {
		Path path = file.toPath();
		try {
			return Files.readAllLines(path);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return null;
		}
	}

	/**
	 * Returns a List<Caption> based on a series of lines parsed using linesToList on a valid captions file.
	 * @param lines the list of lines as output form linesToList()
	 * @return a list of valid Caption objects
	 */
	public static List<Caption> listToCaptions(List<String> lines) {
		List<Caption> captions = new LinkedList<>();
		//manually handle the first caption
		if (!lines.get(0).equals("")) {
			captions.add(parseCaption(lines, 0));
		}
		for (int lineNo = 1; lineNo < lines.size(); lineNo++) {   //start on the first line, avoiding double counting of first caption due to blank first line
			if (lines.get(lineNo).equals("") && lineNo + 1 < lines.size()) {   //if the line is empty and it's not the last line of the document:
				captions.add(parseCaption(lines, lineNo + 1));
			}
		}
		return captions;
	}

	/**
	 * Returns a Caption based on the data following the current position in a List<String> of lines of a captions document. Written as a helper to listToCaptions().
	 * @param lineNumber first line of caption data, i.e. assigned caption number
	 * @return Caption object
	 */
	private static Caption parseCaption(List<String> lines, int lineNumber) {
		String captionNumberString = lines.get(lineNumber);  //the caption number is on the first line
		captionNumberString = captionNumberString.replaceAll("[^a-zA-Z0-9]", "");  //get rid of all non-alphanumeric characters in caption numbers (encoding mismatches) before parsing as int
		int captionNumber = Integer.parseInt(captionNumberString);   //store it as an integer
		String captionTiming = lines.get(lineNumber + 1);  //the timing data is on the line after that,
		//then we start counting content lines
		String captionContent = "";    //nothing in the subtitle to start out with
		int firstContentLine = lineNumber + 2;  //the first line of caption content
		int offset = 0;   //offset counting from firstContentLine
		String currentLine = lines.get(firstContentLine);
		while (!currentLine.equals("")) {    //while there's still content,
			captionContent += currentLine + CAPTION_BREAK_DELINEATOR;
			offset++;
			if (firstContentLine + offset < lines.size()) {  //if the target line is out of bounds
				currentLine = lines.get(firstContentLine + offset);  //stop there
			}
			else {
				break;
			}
		}        //while breaks with offset at index of breakline
		captionContent = captionContent.substring(0, captionContent.length() - CAPTION_BREAK_DELINEATOR.length()); //cut off the trailing caption break delineator
		return new Caption(captionNumber, captionTiming, captionContent);
	}

	/**
	 * Searches all Caption content in a specified List for a specified String (case-insensitively) and replaces each instance with another String.
	 * @param captions The list of captions to be used
	 * @param search The String to be replaced
	 * @param replace The String with which to replace
	 * @return The number of successful replacements performed
	 */
	private static int searchAndReplace(List<Caption> captions, String search, String replace) {
		if (ENABLE_CONSOLE_OUTPUT) System.out.print("Replacing \"" + search + "\" with \"" + replace + "\"... ");
		int replacementsPerformed = 0;
		String target = "(?i)" + search;
		
		for (Caption caption : captions) {
			String originalContent = caption.getContent();
			String unreplaced = originalContent;
			String replaced = originalContent.replaceFirst(target, replace);
			while (unreplaced != replaced) {  //if something got replaced
				unreplaced = replaced;
				replaced = replaced.replaceFirst(target, replace); //try it again
				replacementsPerformed++;    //and note that something got replaced
			}
			caption.setContent(replaced);
		}

		if (ENABLE_CONSOLE_OUTPUT) System.out.println(replacementsPerformed + " replacements performed");
		return replacementsPerformed;
	}

	/**
	 * Removes duplicate/excess consecutive spaces from a List of Captions.
	 * @param captions List of Captions to be analyzed
	 * @return The total number of excess spaces removed from all captions 
	 */
	private static int removeMultipleSpaces(List<Caption> captions) {
		if (ENABLE_CONSOLE_OUTPUT) System.out.print("Removing multiple spaces... ");
		int spacesRemoved = 0;

		for (Caption caption : captions) {
			char[] temp = caption.getContent().toCharArray();  //store the chars in an array temporarily
			List<Character> chars = new LinkedList<>();
			for (char c : temp) {    //copy the array into a list of chars
				chars.add(c);
			}
			List<Integer> queuedForRemoval = new LinkedList<Integer>();   
			for (int i = 0; i < chars.size() - 1; i++) {
				if (chars.get(i) == ' ' && chars.get(i + 1) == ' ') { //if this and the next character are spaces
					queuedForRemoval.add(i);   //queue the current index for removal
					spacesRemoved++;
				}
			}
			for (int i = queuedForRemoval.size() - 1; i >= 0; i--) {  //remove all queued indexes, highest first
				chars.remove((int) queuedForRemoval.get(i));
			}
			String newCaptionContent = "";   //write new string with remaining chars
			for (char c : chars) {
				newCaptionContent = newCaptionContent + c;
			}
			caption.setContent(newCaptionContent);  //update caption contents
		}

		if (ENABLE_CONSOLE_OUTPUT) System.out.println(spacesRemoved + " extra spaces removed");
		return spacesRemoved;
	}

	/**
	 * Removes unnecessary spaces from the beginning and end of Caption content Strings.
	 * @param captions The List of Captions to be analyzed
	 * @return The total number of successful trim actions performed
	 */
	private static int trimTrailingSpaces(List<Caption> captions) {
		if (ENABLE_CONSOLE_OUTPUT) System.out.print("Removing trailing spaces... ");
		int spacesRemoved = 0;
		
		for (Caption caption : captions) {
			String originalContent = caption.getContent();
			String newContent = originalContent.trim();
			if (!originalContent.equals(newContent)) {
				spacesRemoved++;
			}
			caption.setContent(newContent);
		}
		
		if (ENABLE_CONSOLE_OUTPUT) System.out.println(spacesRemoved + " successful trims performed");
		return spacesRemoved;
	}
	
	/**
	 * Capitalizes the first letter in sentences of caption content. Should be used after trimTrailingSpaces to ensure first sentence is capitalized.
	 * @param captions The List of Captions to be analyzed
	 * @return the total number of capitalizations performed in all captions
	 */
	private static int capitalizeFirstLetters(List<Caption> captions) {
		if (ENABLE_CONSOLE_OUTPUT) System.out.print("Auto-capitalizing sentences... ");
		int capitalizationsPerformed = 0;
		boolean firstCaption = true;
		boolean nextShouldCapitalize = false;
		
		for (Caption caption : captions) {
			char[] temp = caption.getContent().toCharArray();  //store the chars in an array temporarily
			List<Character> chars = new LinkedList<>();
			for (char c : temp) {    //copy the array into a list of chars
				chars.add(c);
			}
			if ((chars.size() != 0 && nextShouldCapitalize) || firstCaption) {  //if we're sure it's supposed to be a new sentence, or it's the first caption of the file:
				char upper = Character.toUpperCase(chars.get(FIRST_CHARACTER));  //capitalize the first character
				chars.remove(FIRST_CHARACTER);
				chars.add(0, upper);
				capitalizationsPerformed++;
			}
			for (int i = 0; i < chars.size() - 1; i++) {
				if (chars.get(i) == '.' && chars.get(i + 1) == ' ') { //if this character is a period and the next character is a space,
					if (i + 2 < chars.size()) {    //and the character after that exists,
						char upper = Character.toUpperCase(chars.get(i + 2));
						chars.remove(i + 2);    //replace the character after that with the corresponding capitalized character
						chars.add(i + 2, upper);
						capitalizationsPerformed++;
					}
				}
			}
			String newCaptionContent = "";   //write new string with updated chars
			for (char c : chars) {
				newCaptionContent = newCaptionContent + c;
			}
			if (newCaptionContent.substring(newCaptionContent.length() - 1).equals(".")) {
				nextShouldCapitalize = true;
			}
			else nextShouldCapitalize = false;
			
			caption.setContent(newCaptionContent);  //update caption contents
			firstCaption = false;
		}

		if (ENABLE_CONSOLE_OUTPUT) System.out.println(capitalizationsPerformed + " capitalizations performed");
		return capitalizationsPerformed;
	}
	
	/**
	 * Prints the specified List of Captions with correct formatting.
	 * @param captions The List of Captions to be printed
	 */
	public static void printCaptions(List<Caption> captions) {
		if (ENABLE_CONSOLE_OUTPUT) System.out.println("Caption output:");
		for (Caption caption : captions) {
			System.out.println(String.valueOf(caption.getNumber()));
			System.out.println(caption.getTiming());
			System.out.println(caption.getContent());
			System.out.println("");
		}
		if (ENABLE_CONSOLE_OUTPUT) System.out.println("Caption output end");
	}


	public static void main(String[] args) {
		//file selection and parsing
		File file = InputHandler.chooseFile();
		List<String> lines = fileToList(file);
		List<Caption> captions = listToCaptions(lines);
		//edits performed
		searchAndReplace(captions, "space", "yiker");
		searchAndReplace(captions, "peer to peer", "peer-to-peer");
		removeMultipleSpaces(captions);
		trimTrailingSpaces(captions);
		capitalizeFirstLetters(captions);
		//output handled
		if (InputHandler.userConfirm("Console preview", "Post caption preview to console?")) {
			printCaptions(captions);
		}
		String fileName = InputHandler.userInput("Output filename (with extension):");
		if (fileName != null) {
			Path outPath = InputHandler.writeCaptionFile(captions, fileName);
			if (ENABLE_CONSOLE_OUTPUT) System.out.println("New caption file " + outPath.getFileName() + " generated");
		}
		else if (ENABLE_CONSOLE_OUTPUT) System.out.println("No caption file generated");
		//complete
		System.out.print("\nDone!");
	}

}

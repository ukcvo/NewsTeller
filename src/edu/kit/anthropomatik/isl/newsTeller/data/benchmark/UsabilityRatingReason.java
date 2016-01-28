package edu.kit.anthropomatik.isl.newsTeller.data.benchmark;

/**
 * Represents the various reasons for rating an event as usable or not in the benchmark.
 * 
 * @author Lucas Bechberger (ukcvo@student.kit.edu, bechberger@fbk.eu)
 *
 */
public enum UsabilityRatingReason {
	USABLE,
	NO_EVENT,
	KEYWORD_ENTITY_CATEGORIZATION,
	MISSING_OBJECT,
	OVERLAPPING_CONSTITUENTS,
	EVENT_MERGE,
	MISSING_SUBJECT,
	BROKEN_CONSTITUENT,
	WRONG_PARSE,
	OTHER_ENTITY_CATEGORIZATION,
	TOO_MANY_ACTORS,
	MISSING_LOCATION,
	KEYWORD_REGEX_MISMATCH;
	
	/**
	 * Returns the number representation as a String.
	 */
	public String toNumberString() {
		switch (this) {
		case USABLE:
			return "1";
		case NO_EVENT:
			return "2";
		case KEYWORD_ENTITY_CATEGORIZATION:
			return "3";
		case MISSING_OBJECT:
			return "4";
		case OVERLAPPING_CONSTITUENTS:
			return "5";
		case EVENT_MERGE:
			return "6";
		case MISSING_SUBJECT:
			return "7";
		case BROKEN_CONSTITUENT:
			return "8";
		case WRONG_PARSE:
			return "9";
		case OTHER_ENTITY_CATEGORIZATION:
			return "10";
		case TOO_MANY_ACTORS:
			return "11";
		case MISSING_LOCATION:
			return "12";
		case KEYWORD_REGEX_MISMATCH:
			return "13";
		default:
			return "Error";
		}
	}
	
	/**
	 * Converts the integer from the csv file into an enum.
	 */
	public static UsabilityRatingReason fromInteger(int i) {
		switch (i) {
		case 1:
			return USABLE;
		case 2:
			return NO_EVENT;
		case 3:
			return KEYWORD_ENTITY_CATEGORIZATION;
		case 4:
			return MISSING_OBJECT;
		case 5:
			return OVERLAPPING_CONSTITUENTS;
		case 6:
			return EVENT_MERGE;
		case 7:
			return MISSING_SUBJECT;
		case 8:	
			return BROKEN_CONSTITUENT;
		case 9:
			return WRONG_PARSE;
		case 10:
			return OTHER_ENTITY_CATEGORIZATION;
		case 11:
			return TOO_MANY_ACTORS;
		case 12:	
			return MISSING_LOCATION;
		case 13:
			return KEYWORD_REGEX_MISMATCH;
		default:
			return null;
		}
	}
}

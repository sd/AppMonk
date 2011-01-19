package appmonk.tricks;

import java.util.regex.Pattern;

public class PatternTricks {
	public static final String EXP_PHONE_NUMBER_USA = "^\\(?\\d{3}\\)?[- ]?\\d{3}[- ]?\\d{4}$";
	public static final String EXP_EMAIL_LESS_STRICT = "^[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";
	public static final String EXP_EMAIL_MORE_STRICT = "^[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}$";

	public static boolean isPhoneNumberValid(String phoneNumber) {
		return Pattern.matches(EXP_PHONE_NUMBER_USA, phoneNumber);
	}
	
	//Strict checking means that the domain will be limited to 4 characters instead of 6
	//The only domain that should be a problem in this case .museum
	public static boolean isEmailValid(String email, boolean moreStrict) {
		return Pattern.matches((moreStrict ? EXP_EMAIL_MORE_STRICT : EXP_EMAIL_LESS_STRICT), email);
	}

}

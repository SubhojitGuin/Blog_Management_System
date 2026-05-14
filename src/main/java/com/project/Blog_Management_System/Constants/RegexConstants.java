package com.project.Blog_Management_System.Constants;

public final class RegexConstants {

    public static final String CATEGORY_NAME = "^[a-zA-Z0-9\\s.-]+$";
    public static final String CATEGORY_SLUG = "^[a-z0-9]+(?:-[a-z0-9]+)*$";

    public static final String EMAIL_OR_USERNAME = "(^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$)|(^\\w{3,30}$)";
    public static final String USERNAME = "^\\w{3,30}$";
    public static final String PASSWORD = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&-+=()])(?=\\S+$).{8,20}$";

    public static final String DOB = "yyyy-MM-dd";

}

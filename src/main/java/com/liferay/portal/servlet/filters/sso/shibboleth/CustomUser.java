package com.liferay.portal.servlet.filters.sso.shibboleth;

public class CustomUser {

	public CustomUser(String userId, String firstName, String surName, String email, String affiliation) {
		super();
		this.userId = userId;
		this.firstName = firstName;
		this.surName = surName;
		this.email = email;
		this.affiliation = affiliation;
	}
	
	private String userId;
	private String firstName;
	private String surName;
	private String email;
	private String affiliation;
	
	public String getUserId() {
		return userId;
	}
	public String getFirstName() {
		return firstName;
	}
	public String getSurName() {
		return surName;
	}
	public String getEmail() {
		return email;
	}
	public String getAffiliation() {
		return affiliation;
	}
	
	public boolean isValid() {
		boolean result = false;
		if (userId != null && userId.length() > 0
				&& firstName != null && firstName.length() > 0
				&& surName != null && surName.length() > 0
				&& email != null && email.length() > 0
				&& affiliation != null && affiliation.length() > 0) {
			result = true;
		}
			
		return result;
	}
	@Override
	public String toString() {
		return "CustomUser [userId=" + userId + ", firstName=" + firstName + ", surName=" + surName + ", email=" + email
				+ ", affiliation=" + affiliation + "]";
	}
	
}

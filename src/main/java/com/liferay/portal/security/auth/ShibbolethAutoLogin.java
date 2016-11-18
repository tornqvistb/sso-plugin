package com.liferay.portal.security.auth;

import java.util.Calendar;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.PrefsPropsUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.Role;
import com.liferay.portal.model.RoleConstants;
import com.liferay.portal.model.User;
import com.liferay.portal.service.RoleLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.servlet.filters.sso.shibboleth.CustomUser;
import com.liferay.portal.shibboleth.util.CorruptStringFixer;
import com.liferay.portal.shibboleth.util.ShibbolethPropsKeys;
import com.liferay.portal.shibboleth.util.Util;
import com.liferay.portal.util.PortalUtil;

/**
 * Performs autologin based on the header values passed by Shibboleth.
 * 
 * The Shibboleth user ID header set in the configuration must contain the user
 * ID, if users are authenticated by screen name or the user email, if the users
 * are authenticated by email (Portal settings --> Authentication --> General).
 * 
 * @author Romeo Sheshi
 * @author Ivan Novakov <ivan.novakov@debug.cz>
 */
public class ShibbolethAutoLogin implements AutoLogin {

	private static Log _log = LogFactoryUtil.getLog(ShibbolethAutoLogin.class);

	public String[] login(HttpServletRequest req, HttpServletResponse res) throws AutoLoginException {

		User user = null;
		String[] credentials = null;
		long companyId = PortalUtil.getCompanyId(req);

		try {
			
			_log.debug("Shibboleth Autologin - company id: " + companyId);
			_log.debug("Shibboleth Autologin - header user id attribute: " + PrefsPropsUtil.getString(companyId, ShibbolethPropsKeys.SHIBBOLETH_HEADER));
			_log.debug("Shibboleth Autologin - header email attribute: " + PrefsPropsUtil.getString(companyId, ShibbolethPropsKeys.SHIBBOLETH_HEADER_EMAIL));
			_log.debug("Shibboleth Autologin - header firstname attribute: " + PrefsPropsUtil.getString(companyId, ShibbolethPropsKeys.SHIBBOLETH_HEADER_FIRSTNAME));
			_log.debug("Shibboleth Autologin - header surname attribute: " + PrefsPropsUtil.getString(companyId, ShibbolethPropsKeys.SHIBBOLETH_HEADER_SURNAME));
			_log.debug("Shibboleth Autologin - header affiliation attribute: " + PrefsPropsUtil.getString(companyId, ShibbolethPropsKeys.SHIBBOLETH_HEADER_AFFILIATION));
			
			if (!Util.isEnabled(companyId)) {
				_log.debug("Shibboleth Autologin - company id is not enabled ");
				return credentials;				
			}
			
			CorruptStringFixer fixer = new CorruptStringFixer();
			_log.debug("Shibboleth Autologin - firstname: " + req.getHeader(PrefsPropsUtil.getString(companyId, ShibbolethPropsKeys.SHIBBOLETH_HEADER_FIRSTNAME)));
			_log.debug("Shibboleth Autologin - surname: " + req.getHeader(PrefsPropsUtil.getString(companyId, ShibbolethPropsKeys.SHIBBOLETH_HEADER_SURNAME)));
			
			String firstName = fixer.fixString(req.getHeader(PrefsPropsUtil.getString(companyId, ShibbolethPropsKeys.SHIBBOLETH_HEADER_FIRSTNAME)));
			String surName = fixer.fixString(req.getHeader(PrefsPropsUtil.getString(companyId, ShibbolethPropsKeys.SHIBBOLETH_HEADER_SURNAME)));
			
			CustomUser customUser = new CustomUser(req.getHeader(PrefsPropsUtil.getString(companyId, ShibbolethPropsKeys.SHIBBOLETH_HEADER)),
					firstName,
					surName,
					req.getHeader(PrefsPropsUtil.getString(companyId, ShibbolethPropsKeys.SHIBBOLETH_HEADER_EMAIL)),
					req.getHeader(PrefsPropsUtil.getString(companyId, ShibbolethPropsKeys.SHIBBOLETH_HEADER_AFFILIATION)));
			
			_log.debug("CustomUser from request: " + customUser.toString());
			
			if (!customUser.isValid()) {
				_log.debug("Shibboleth Autologin - user from request header is not valid: " + customUser.toString());
				return credentials;								
			}
			
			user = loginFromRequest(companyId, customUser);
			if (Validator.isNull(user)) {
				return credentials;
			}

			credentials = new String[3];
			credentials[0] = String.valueOf(user.getUserId());
			credentials[1] = user.getPassword();
			credentials[2] = Boolean.TRUE.toString();
			return credentials;

		} catch (NoSuchUserException e) {
			logError(e);
		} catch (Exception e) {
			logError(e);
			throw new AutoLoginException(e);
		}

		return credentials;
	}

	private User loginFromRequest(long companyId, CustomUser customUser) throws Exception {
		User user = null;


		try {
			user = UserLocalServiceUtil.getUserByScreenName(companyId, customUser.getUserId());

			_log.info("User found: " + user.getScreenName() + " (" + user.getEmailAddress() + ")");

			if (Util.autoUpdateUser(companyId)) {
				_log.info("Auto-updating user...");
				updateUser(user, customUser);
			}

		} catch (NoSuchUserException e) {
			_log.error("User not found");

			if (Util.autoCreateUser(companyId)) {
				_log.info("Importing user from session...");
				user = addUser(companyId, customUser);
				_log.info("Created user with ID: " + user.getUserId());
				addDefaultRolesToUser(companyId, user);
				System.out.println("loginFromRequest 1");
				user = UserLocalServiceUtil.getUserByScreenName(companyId, customUser.getUserId());
				System.out.println("loginFromRequest 2");
				String roles = "Roles after creation: ";
				for (Role role : user.getRoles()) {
					roles = roles + ", " + role.getDescription();
				}
				_log.info(roles);
				System.out.println(roles);
			}
		}

		return user;
	}
	
	private User addUser(long companyId, CustomUser customUser)
			throws Exception {

		long creatorUserId = 0;
		boolean autoPassword = false;
		String password1 = "!pqq678%!!";
		String password2 = "!pqq678%!!";
		boolean autoScreenName = false;
		long facebookId = 0;
		String openId = StringPool.BLANK;
		Locale locale = Locale.US;
		String middleName = StringPool.BLANK;
		int prefixId = 0;
		int suffixId = 0;
		boolean male = true;
		int birthdayMonth = Calendar.JANUARY;
		int birthdayDay = 1;
		int birthdayYear = 1970;
		// Put companycode in jobTitle field for now.
		String jobTitle = customUser.getAffiliation();
		

		long[] groupIds = null;
		long[] organizationIds = null;
		long[] roleIds = null;
		long[] userGroupIds = null;

		boolean sendEmail = false;
		ServiceContext serviceContext = null;

		User user = UserLocalServiceUtil.addUser(creatorUserId, companyId, autoPassword, password1, password2,
				autoScreenName, customUser.getUserId(), customUser.getEmail(), facebookId, openId, locale, customUser.getFirstName(), middleName, customUser.getSurName(),
				prefixId, suffixId, male, birthdayMonth, birthdayDay, birthdayYear, jobTitle, groupIds,
				organizationIds, roleIds, userGroupIds, sendEmail, serviceContext);
		UserLocalServiceUtil.updatePasswordReset(user.getUserId(), false);
		return user;
	}

	private void updateUser(User user, CustomUser customUser) throws Exception {

		String emailAddress = customUser.getEmail();
		if (!Validator.isNull(emailAddress) && !user.getEmailAddress().equals(emailAddress)) {
			_log.info("User [" + user.getScreenName() + "]: update email address [" + user.getEmailAddress()
					+ "] --> [" + emailAddress + "]");
			user.setEmailAddress(emailAddress);
		}

		String firstname = customUser.getFirstName();
		if (!Validator.isNull(firstname) && !user.getFirstName().equals(firstname)) {
			_log.info("User [" + user.getScreenName() + "]: update first name [" + user.getFirstName() + "] --> ["
					+ firstname + "]");
			user.setFirstName(firstname);
		}

		String surname = customUser.getSurName();
		if (!Validator.isNull(surname) && !user.getLastName().equals(surname)) {
			_log.info("User [" + user.getScreenName() + "]: update last name [" + user.getLastName() + "] --> ["
					+ surname + "]");
			user.setLastName(surname);
		}

		String affiliation = customUser.getAffiliation();
		if (!Validator.isNull(affiliation) && !user.getJobTitle().equals(affiliation)) {
			_log.info("User [" + user.getScreenName() + "]: update job title (affiliation) [" + user.getJobTitle() + "] --> ["
					+ affiliation + "]");
			user.setJobTitle(affiliation);
		}
		
		UserLocalServiceUtil.updateUser(user);
	}

	private void addDefaultRolesToUser(long companyId, User user) throws Exception {
		_log.info("addDefaultRolesToUser: get USER role");
		Role userRole = RoleLocalServiceUtil.getRole(companyId, RoleConstants.USER);
		_log.info("addDefaultRolesToUser: clear user roles");
		RoleLocalServiceUtil.clearUserRoles(user.getUserId());
		_log.info("addDefaultRolesToUser: add user role");
		RoleLocalServiceUtil.addUserRole(user.getUserId(), userRole);
		_log.info("addDefaultRolesToUser: done");
	}

	private void logError(Exception e) {
		_log.error("Exception message = " + e.getMessage() + " cause = " + e.getCause());
		if (_log.isDebugEnabled()) {
			e.printStackTrace();
		}

	}

public String[] handleException(HttpServletRequest arg0, HttpServletResponse arg1, Exception arg2)
		throws AutoLoginException {
	// TODO Auto-generated method stub
	return null;
}

}
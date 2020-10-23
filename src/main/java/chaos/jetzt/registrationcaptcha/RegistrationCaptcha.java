package chaos.jetzt.registrationcaptcha;

import org.keycloak.Config;
import org.keycloak.authentication.FormAction;
import org.keycloak.authentication.FormActionFactory;
import org.keycloak.authentication.FormContext;
import org.keycloak.authentication.ValidationContext;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.messages.Messages;
//import org.keycloak.services.validation.Validation;

import javax.ws.rs.core.MultivaluedMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RegistrationCaptcha implements FormAction, FormActionFactory, ConfiguredProvider {
    public static final String CAPTCHA_RESPONSE = "captcha-response";
    public static final String CAPTCHA_REFERENCE_CATEGORY = "captcha";
    public static final String CAPTCHA_QUESTION = "question";
    public static final String CAPTCHA_ANSWER = "answer";

    public static final String PROVIDER_ID = "registration-captcha-action";


    @Override
    public String getDisplayType() {
        return "Captcha";
    }

    @Override
    public String getReferenceCategory() {
        return CAPTCHA_REFERENCE_CATEGORY;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    private static AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.DISABLED
    };
    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public void buildPage(FormContext context, LoginFormsProvider form) {
        AuthenticatorConfigModel captchaConfig = context.getAuthenticatorConfig();
        String userLanguageTag = context.getSession().getContext().resolveLocale(context.getUser()).toLanguageTag();
        if (captchaConfig == null || captchaConfig.getConfig() == null
                || captchaConfig.getConfig().get(CAPTCHA_ANSWER) == null
                || captchaConfig.getConfig().get(CAPTCHA_QUESTION) == null
        ) {
            form.addError(new FormMessage(null, Messages.RECAPTCHA_NOT_CONFIGURED));
            return;
        }

        form.setAttribute("captchaRequired", true);
        form.setAttribute("captchaQuestion", captchaConfig.getConfig().get(CAPTCHA_QUESTION));
    }

    @Override
    public void validate(ValidationContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        List<FormMessage> errors = new ArrayList<>();
        boolean success = false;
        context.getEvent().detail(Details.REGISTER_METHOD, "form");

        String captcha = formData.getFirst(CAPTCHA_RESPONSE);
//        if (!Validation.isBlank(captcha)) {
        AuthenticatorConfigModel captchaConfig = context.getAuthenticatorConfig();
        String answer = captchaConfig.getConfig().get(CAPTCHA_ANSWER);

        success = validateCaptcha(context, success, captcha, answer);
//        }
        if (success) {
            context.success();
        } else {
            errors.add(new FormMessage(null, Messages.RECAPTCHA_FAILED));
            formData.remove(CAPTCHA_RESPONSE);
            context.error(Errors.INVALID_REGISTRATION);
            context.validationError(formData, errors);
            context.excludeOtherErrors();
            return;
        }
    }

    protected boolean validateCaptcha(ValidationContext context, boolean success, String captcha, String answerString) {
        answerString = answerString.replace(" ", "");
        List<String> answeres = Arrays.asList(answerString.split(","));

        captcha = captcha.replace(" ", "");

        for (String answer: answeres) {
            if (captcha.equalsIgnoreCase(answer)) {
                success = Boolean.TRUE;
            }
        }

        return success;
    }

    @Override
    public void success(FormContext formContext) {

    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {

    }

    @Override
    public String getHelpText() {
        return "Adds a simple self-configurable captcha.  Captchas verify that the entity that is registering is a human.  This can only be used on the internet and must be configured after you add it.";
    }

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<ProviderConfigProperty>();

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(CAPTCHA_QUESTION);
        property.setLabel("Captcha Question");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("Question to be asked");
        CONFIG_PROPERTIES.add(property);
        property = new ProviderConfigProperty();
        property.setName(CAPTCHA_ANSWER);
        property.setLabel("Captcha Answer");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("Answers to the Question (seperated by commas)");
        CONFIG_PROPERTIES.add(property);
    }


    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public FormAction create(KeycloakSession keycloakSession) {
        return this;
    }

    @Override
    public void init(Config.Scope scope) {

    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}

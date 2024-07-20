/*
 * #%L
 * HAPI FHIR - Core Library
 * %%
 * Copyright (C) 2014 - 2024 Smile CDR, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ca.uhn.fhir.context.support;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.util.ParametersUtil;
import ca.uhn.fhir.util.UrlUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * This interface is a version-independent representation of the
 * various functions that can be provided by validation and terminology
 * services.
 * <p>
 * This interface is invoked directly by internal parts of the HAPI FHIR API, including the
 * Validator and the FHIRPath evaluator. It is used to supply artifacts required for validation
 * (e.g. StructureDefinition resources, ValueSet resources, etc.) and also to provide
 * terminology functions such as code validation, ValueSet expansion, etc.
 * </p>
 * <p>
 * Implementations are not required to implement all of the functions
 * in this interface; in fact it is expected that most won't. Any
 * methods which are not implemented may simply return <code>null</code>
 * and calling code is expected to be able to handle this. Generally, a
 * series of implementations of this interface will be joined together using
 * the
 * <a href="https://hapifhir.io/hapi-fhir/apidocs/hapi-fhir-validation/org/hl7/fhir/common/hapi/validation/support/ValidationSupportChain.html">ValidationSupportChain</a>
 * class.
 * </p>
 * <p>
 * See <a href="https://hapifhir.io/hapi-fhir/docs/validation/validation_support_modules.html">Validation Support Modules</a>
 * for information on how to assemble and configure implementations of this interface. See also
 * the <code>org.hl7.fhir.common.hapi.validation.support</code>
 * <a href="./package-summary.html">package summary</a>
 * in the <code>hapi-fhir-validation</code> module for many implementations of this interface.
 * </p>
 *
 * @since 5.0.0
 */
public interface IValidationSupport {
	String URL_PREFIX_VALUE_SET = "http://hl7.org/fhir/ValueSet/";

	/**
	 * Expands the given portion of a ValueSet
	 *
	 * @param theValidationSupportContext The validation support module will be passed in to this method. This is convenient in cases where the operation needs to make calls to
	 *                                    other method in the support chain, so that they can be passed through the entire chain. Implementations of this interface may always safely ignore this parameter.
	 * @param theExpansionOptions         If provided (can be <code>null</code>), contains options controlling the expansion
	 * @param theValueSetToExpand         The valueset that should be expanded
	 * @return The expansion, or null
	 */
	@Nullable
	default ValueSetExpansionOutcome expandValueSet(
			ValidationSupportContext theValidationSupportContext,
			@Nullable ValueSetExpansionOptions theExpansionOptions,
			@Nonnull IBaseResource theValueSetToExpand) {
		return null;
	}

	/**
	 * Expands the given portion of a ValueSet by canonical URL.
	 *
	 * @param theValidationSupportContext The validation support module will be passed in to this method. This is convenient in cases where the operation needs to make calls to
	 *                                    other method in the support chain, so that they can be passed through the entire chain. Implementations of this interface may always safely ignore this parameter.
	 * @param theExpansionOptions         If provided (can be <code>null</code>), contains options controlling the expansion
	 * @param theValueSetUrlToExpand      The valueset that should be expanded
	 * @return The expansion, or null
	 * @throws ResourceNotFoundException If no ValueSet can be found with the given URL
	 * @since 6.0.0
	 */
	@Nullable
	default ValueSetExpansionOutcome expandValueSet(
			ValidationSupportContext theValidationSupportContext,
			@Nullable ValueSetExpansionOptions theExpansionOptions,
			@Nonnull String theValueSetUrlToExpand)
			throws ResourceNotFoundException {
		Validate.notBlank(theValueSetUrlToExpand, "theValueSetUrlToExpand must not be null or blank");
		IBaseResource valueSet = fetchValueSet(theValueSetUrlToExpand);
		if (valueSet == null) {
			throw new ResourceNotFoundException(
					Msg.code(2024) + "Unknown ValueSet: " + UrlUtil.escapeUrlParam(theValueSetUrlToExpand));
		}
		return expandValueSet(theValidationSupportContext, theExpansionOptions, valueSet);
	}

	/**
	 * Load and return all conformance resources associated with this
	 * validation support module. This method may return null if it doesn't
	 * make sense for a given module.
	 */
	@Nullable
	default List<IBaseResource> fetchAllConformanceResources() {
		return null;
	}

	/**
	 * Load and return all possible search parameters
	 *
	 * @since 6.6.0
	 */
	@Nullable
	default <T extends IBaseResource> List<T> fetchAllSearchParameters() {
		return null;
	}

	/**
	 * Load and return all possible structure definitions
	 */
	@Nullable
	default <T extends IBaseResource> List<T> fetchAllStructureDefinitions() {
		return null;
	}

	/**
	 * Load and return all possible structure definitions aside from resource definitions themselves
	 */
	@Nullable
	default <T extends IBaseResource> List<T> fetchAllNonBaseStructureDefinitions() {
		List<T> retVal = fetchAllStructureDefinitions();
		if (retVal != null) {
			List<T> newList = new ArrayList<>(retVal.size());
			for (T next : retVal) {
				String url = defaultString(getFhirContext().newTerser().getSinglePrimitiveValueOrNull(next, "url"));
				if (url.startsWith("http://hl7.org/fhir/StructureDefinition/")) {
					String lastPart = url.substring("http://hl7.org/fhir/StructureDefinition/".length());
					if (getFhirContext().getResourceTypes().contains(lastPart)) {
						continue;
					}
				}

				newList.add(next);
			}

			retVal = newList;
		}

		return retVal;
	}

	/**
	 * Fetch a code system by ID
	 *
	 * @param theSystem The code system
	 * @return The valueset (must not be null, but can be an empty ValueSet)
	 */
	@Nullable
	default IBaseResource fetchCodeSystem(String theSystem) {
		return null;
	}

	/**
	 * Loads a resource needed by the validation (a StructureDefinition, or a
	 * ValueSet)
	 *
	 * <p>
	 * Note: Since 5.3.0, {@literal theClass} can be {@literal null}
	 * </p>
	 *
	 * @param theClass The type of the resource to load, or <code>null</code> to return any resource with the given canonical URI
	 * @param theUri   The resource URI
	 * @return Returns the resource, or <code>null</code> if no resource with the
	 * given URI can be found
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	default <T extends IBaseResource> T fetchResource(@Nullable Class<T> theClass, String theUri) {
		Validate.notBlank(theUri, "theUri must not be null or blank");

		if (theClass == null) {
			Supplier<IBaseResource>[] sources = new Supplier[] {
				() -> fetchStructureDefinition(theUri), () -> fetchValueSet(theUri), () -> fetchCodeSystem(theUri)
			};
			return (T) Arrays.stream(sources)
					.map(t -> t.get())
					.filter(t -> t != null)
					.findFirst()
					.orElse(null);
		}

		switch (getFhirContext().getResourceType(theClass)) {
			case "StructureDefinition":
				return theClass.cast(fetchStructureDefinition(theUri));
			case "ValueSet":
				return theClass.cast(fetchValueSet(theUri));
			case "CodeSystem":
				return theClass.cast(fetchCodeSystem(theUri));
		}

		if (theUri.startsWith(URL_PREFIX_VALUE_SET)) {
			return theClass.cast(fetchValueSet(theUri));
		}

		return null;
	}

	@Nullable
	default IBaseResource fetchStructureDefinition(String theUrl) {
		return null;
	}

	/**
	 * Returns <code>true</code> if codes in the given code system can be expanded
	 * or validated
	 *
	 * @param theValidationSupportContext The validation support module will be passed in to this method. This is convenient in cases where the operation needs to make calls to
	 *                                    other method in the support chain, so that they can be passed through the entire chain. Implementations of this interface may always safely ignore this parameter.
	 * @param theSystem                   The URI for the code system, e.g. <code>"http://loinc.org"</code>
	 * @return Returns <code>true</code> if codes in the given code system can be
	 * validated
	 */
	default boolean isCodeSystemSupported(ValidationSupportContext theValidationSupportContext, String theSystem) {
		return false;
	}

	/**
	 * Returns <code>true</code> if a Remote Terminology Service is currently configured
	 *
	 * @return Returns <code>true</code> if a Remote Terminology Service is currently configured
	 */
	default boolean isRemoteTerminologyServiceConfigured() {
		return false;
	}

	/**
	 * Fetch the given ValueSet by URL, or returns null if one can't be found for the given URL
	 */
	@Nullable
	default IBaseResource fetchValueSet(String theValueSetUrl) {
		return null;
	}

	/**
	 * Fetch the given binary data by key.
	 *
	 * @param binaryKey
	 * @return
	 */
	default byte[] fetchBinary(String binaryKey) {
		return null;
	}

	/**
	 * Validates that the given code exists and if possible returns a display
	 * name. This method is called to check codes which are found in "example"
	 * binding fields (e.g. <code>Observation.code</code>) in the default profile.
	 *
	 * @param theValidationSupportContext The validation support module will be passed in to this method. This is convenient in cases where the operation needs to make calls to
	 *                                    other method in the support chain, so that they can be passed through the entire chain. Implementations of this interface may always safely ignore this parameter.
	 * @param theOptions                  Provides options controlling the validation
	 * @param theCodeSystem               The code system, e.g. "<code>http://loinc.org</code>"
	 * @param theCode                     The code, e.g. "<code>1234-5</code>"
	 * @param theDisplay                  The display name, if it should also be validated
	 * @return Returns a validation result object
	 */
	@Nullable
	default CodeValidationResult validateCode(
			ValidationSupportContext theValidationSupportContext,
			ConceptValidationOptions theOptions,
			String theCodeSystem,
			String theCode,
			String theDisplay,
			String theValueSetUrl) {
		return null;
	}

	/**
	 * Validates that the given code exists and if possible returns a display
	 * name. This method is called to check codes which are found in "example"
	 * binding fields (e.g. <code>Observation.code</code>) in the default profile.
	 *
	 * @param theValidationSupportContext The validation support module will be passed in to this method. This is convenient in cases where the operation needs to make calls to
	 *                                    other method in the support chain, so that they can be passed through the entire chain. Implementations of this interface may always safely ignore this parameter.
	 * @param theCodeSystem               The code system, e.g. "<code>http://loinc.org</code>"
	 * @param theCode                     The code, e.g. "<code>1234-5</code>"
	 * @param theDisplay                  The display name, if it should also be validated
	 * @param theValueSet                 The ValueSet to validate against. Must not be null, and must be a ValueSet resource.
	 * @return Returns a validation result object, or <code>null</code> if this validation support module can not handle this kind of request
	 */
	@Nullable
	default CodeValidationResult validateCodeInValueSet(
			ValidationSupportContext theValidationSupportContext,
			ConceptValidationOptions theOptions,
			String theCodeSystem,
			String theCode,
			String theDisplay,
			@Nonnull IBaseResource theValueSet) {
		return null;
	}

	/**
	 * Look up a code using the system and code value.
	 * @deprecated This method has been deprecated in HAPI FHIR 7.0.0. Use {@link IValidationSupport#lookupCode(ValidationSupportContext, LookupCodeRequest)} instead.
	 *
	 * @param theValidationSupportContext The validation support module will be passed in to this method. This is convenient in cases where the operation needs to make calls to
	 *                                    other method in the support chain, so that they can be passed through the entire chain. Implementations of this interface may always safely ignore this parameter.
	 * @param theSystem                   The CodeSystem URL
	 * @param theCode                     The code
	 * @param theDisplayLanguage          Used to filter out the designation by the display language. To return all designation, set this value to <code>null</code>.
	 */
	@Deprecated
	@Nullable
	default LookupCodeResult lookupCode(
			ValidationSupportContext theValidationSupportContext,
			String theSystem,
			String theCode,
			String theDisplayLanguage) {
		return null;
	}

	/**
	 * Look up a code using the system and code value
	 * @deprecated This method has been deprecated in HAPI FHIR 7.0.0. Use {@link IValidationSupport#lookupCode(ValidationSupportContext, LookupCodeRequest)} instead.
	 *
	 * @param theValidationSupportContext The validation support module will be passed in to this method. This is convenient in cases where the operation needs to make calls to
	 *                                    other method in the support chain, so that they can be passed through the entire chain. Implementations of this interface may always safely ignore this parameter.
	 * @param theSystem                   The CodeSystem URL
	 * @param theCode                     The code
	 */
	@Deprecated
	@Nullable
	default LookupCodeResult lookupCode(
			ValidationSupportContext theValidationSupportContext, String theSystem, String theCode) {
		return lookupCode(theValidationSupportContext, theSystem, theCode, null);
	}

	/**
	 * Look up a code using the system, code and other parameters captured in {@link LookupCodeRequest}.
	 * @since 7.0.0
	 *
	 * @param theValidationSupportContext      The validation support module will be passed in to this method. This is convenient in cases where the operation needs to make calls to
	 *                                         other method in the support chain, so that they can be passed through the entire chain. Implementations of this interface may always safely ignore this parameter.
	 * @param theLookupCodeRequest             The parameters used to perform the lookup, including system and code.
	 */
	@Nullable
	default LookupCodeResult lookupCode(
			ValidationSupportContext theValidationSupportContext, @Nonnull LookupCodeRequest theLookupCodeRequest) {
		// TODO: can change to return null once the deprecated methods are removed
		return lookupCode(
				theValidationSupportContext,
				theLookupCodeRequest.getSystem(),
				theLookupCodeRequest.getCode(),
				theLookupCodeRequest.getDisplayLanguage());
	}

	/**
	 * Returns <code>true</code> if the given ValueSet can be validated by the given
	 * validation support module
	 *
	 * @param theValidationSupportContext The validation support module will be passed in to this method. This is convenient in cases where the operation needs to make calls to
	 *                                    other method in the support chain, so that they can be passed through the entire chain. Implementations of this interface may always safely ignore this parameter.
	 * @param theValueSetUrl              The ValueSet canonical URL
	 */
	default boolean isValueSetSupported(ValidationSupportContext theValidationSupportContext, String theValueSetUrl) {
		return false;
	}

	/**
	 * Generate a snapshot from the given differential profile.
	 *
	 * @param theValidationSupportContext The validation support module will be passed in to this method. This is convenient in cases where the operation needs to make calls to
	 *                                    other method in the support chain, so that they can be passed through the entire chain. Implementations of this interface may always safely ignore this parameter.
	 * @return Returns null if this module does not know how to handle this request
	 */
	@Nullable
	default IBaseResource generateSnapshot(
			ValidationSupportContext theValidationSupportContext,
			IBaseResource theInput,
			String theUrl,
			String theWebUrl,
			String theProfileName) {
		return null;
	}

	/**
	 * Returns the FHIR Context associated with this module
	 */
	FhirContext getFhirContext();

	/**
	 * This method clears any temporary caches within the validation support. It is mainly intended for unit tests,
	 * but could be used in non-test scenarios as well.
	 */
	default void invalidateCaches() {
		// nothing
	}

	/**
	 * Attempt to translate the given concept from one code system to another
	 */
	@Nullable
	default TranslateConceptResults translateConcept(TranslateCodeRequest theRequest) {
		return null;
	}

	/**
	 * This field is used by the Terminology Troubleshooting Log to log which validation support module was used for the operation being logged.
	 */
	default String getName() {
		return "Unknown " + getFhirContext().getVersion().getVersion() + " Validation Support";
	}

	enum IssueSeverity {
		/**
		 * The issue caused the action to fail, and no further checking could be performed.
		 */
		FATAL,
		/**
		 * The issue is sufficiently important to cause the action to fail.
		 */
		ERROR,
		/**
		 * The issue is not important enough to cause the action to fail, but may cause it to be performed suboptimally or in a way that is not as desired.
		 */
		WARNING,
		/**
		 * The issue has no relation to the degree of success of the action.
		 */
		INFORMATION
	}

	enum CodeValidationIssueCode {
		NOT_FOUND,
		CODE_INVALID,
		INVALID,
		OTHER
	}

	enum CodeValidationIssueCoding {
		VS_INVALID,
		NOT_FOUND,
		NOT_IN_VS,

		INVALID_CODE,
		INVALID_DISPLAY,
		OTHER
	}

	class CodeValidationIssue {

		private final String myMessage;
		private final IssueSeverity mySeverity;
		private final CodeValidationIssueCode myCode;
		private final CodeValidationIssueCoding myCoding;

		public CodeValidationIssue(
				String theMessage,
				IssueSeverity mySeverity,
				CodeValidationIssueCode theCode,
				CodeValidationIssueCoding theCoding) {
			this.myMessage = theMessage;
			this.mySeverity = mySeverity;
			this.myCode = theCode;
			this.myCoding = theCoding;
		}

		public String getMessage() {
			return myMessage;
		}

		public IssueSeverity getSeverity() {
			return mySeverity;
		}

		public CodeValidationIssueCode getCode() {
			return myCode;
		}

		public CodeValidationIssueCoding getCoding() {
			return myCoding;
		}
	}

	class ConceptDesignation {

		private String myLanguage;
		private String myUseSystem;
		private String myUseCode;
		private String myUseDisplay;
		private String myValue;

		public String getLanguage() {
			return myLanguage;
		}

		public ConceptDesignation setLanguage(String theLanguage) {
			myLanguage = theLanguage;
			return this;
		}

		public String getUseSystem() {
			return myUseSystem;
		}

		public ConceptDesignation setUseSystem(String theUseSystem) {
			myUseSystem = theUseSystem;
			return this;
		}

		public String getUseCode() {
			return myUseCode;
		}

		public ConceptDesignation setUseCode(String theUseCode) {
			myUseCode = theUseCode;
			return this;
		}

		public String getUseDisplay() {
			return myUseDisplay;
		}

		public ConceptDesignation setUseDisplay(String theUseDisplay) {
			myUseDisplay = theUseDisplay;
			return this;
		}

		public String getValue() {
			return myValue;
		}

		public ConceptDesignation setValue(String theValue) {
			myValue = theValue;
			return this;
		}
	}

	abstract class BaseConceptProperty {
		private final String myPropertyName;

		/**
		 * Constructor
		 */
		protected BaseConceptProperty(String thePropertyName) {
			myPropertyName = thePropertyName;
		}

		public String getPropertyName() {
			return myPropertyName;
		}

		public abstract String getType();
	}

	// The reason these cannot be declared within an enum is because a Remote Terminology Service
	// can support arbitrary types. We do not restrict against the types in the spec.
	// Some of the types in the spec are not yet implemented as well.
	// @see https://github.com/hapifhir/hapi-fhir/issues/5700
	String TYPE_STRING = "string";
	String TYPE_CODING = "Coding";
	String TYPE_GROUP = "group";

	class StringConceptProperty extends BaseConceptProperty {
		private final String myValue;

		/**
		 * Constructor
		 *
		 * @param theName The name
		 */
		public StringConceptProperty(String theName, String theValue) {
			super(theName);
			myValue = theValue;
		}

		public String getValue() {
			return myValue;
		}

		public String getType() {
			return TYPE_STRING;
		}
	}

	class CodingConceptProperty extends BaseConceptProperty {
		private final String myCode;
		private final String myCodeSystem;
		private final String myDisplay;

		/**
		 * Constructor
		 *
		 * @param theName The name
		 */
		public CodingConceptProperty(String theName, String theCodeSystem, String theCode, String theDisplay) {
			super(theName);
			myCodeSystem = theCodeSystem;
			myCode = theCode;
			myDisplay = theDisplay;
		}

		public String getCode() {
			return myCode;
		}

		public String getCodeSystem() {
			return myCodeSystem;
		}

		public String getDisplay() {
			return myDisplay;
		}

		public String getType() {
			return TYPE_CODING;
		}
	}

	class GroupConceptProperty extends BaseConceptProperty {
		public GroupConceptProperty(String thePropertyName) {
			super(thePropertyName);
		}

		private List<BaseConceptProperty> subProperties;

		public BaseConceptProperty addSubProperty(BaseConceptProperty theProperty) {
			if (subProperties == null) {
				subProperties = new ArrayList<>();
			}
			subProperties.add(theProperty);
			return this;
		}

		public List<BaseConceptProperty> getSubProperties() {
			return subProperties != null ? subProperties : Collections.emptyList();
		}

		@Override
		public String getType() {
			return TYPE_GROUP;
		}
	}

	class CodeValidationResult {
		public static final String SOURCE_DETAILS = "sourceDetails";
		public static final String RESULT = "result";
		public static final String MESSAGE = "message";
		public static final String DISPLAY = "display";

		private String myCode;
		private String myMessage;
		private IssueSeverity mySeverity;
		private String myCodeSystemName;
		private String myCodeSystemVersion;
		private List<BaseConceptProperty> myProperties;
		private String myDisplay;
		private String mySourceDetails;

		private List<CodeValidationIssue> myCodeValidationIssues;

		public CodeValidationResult() {
			super();
		}

		/**
		 * This field may contain information about what the source of the
		 * validation information was.
		 */
		public String getSourceDetails() {
			return mySourceDetails;
		}

		/**
		 * This field may contain information about what the source of the
		 * validation information was.
		 */
		public CodeValidationResult setSourceDetails(String theSourceDetails) {
			mySourceDetails = theSourceDetails;
			return this;
		}

		public String getDisplay() {
			return myDisplay;
		}

		public CodeValidationResult setDisplay(String theDisplay) {
			myDisplay = theDisplay;
			return this;
		}

		public String getCode() {
			return myCode;
		}

		public CodeValidationResult setCode(String theCode) {
			myCode = theCode;
			return this;
		}

		String getCodeSystemName() {
			return myCodeSystemName;
		}

		public CodeValidationResult setCodeSystemName(String theCodeSystemName) {
			myCodeSystemName = theCodeSystemName;
			return this;
		}

		public String getCodeSystemVersion() {
			return myCodeSystemVersion;
		}

		public CodeValidationResult setCodeSystemVersion(String theCodeSystemVersion) {
			myCodeSystemVersion = theCodeSystemVersion;
			return this;
		}

		public String getMessage() {
			return myMessage;
		}

		public CodeValidationResult setMessage(String theMessage) {
			myMessage = theMessage;
			return this;
		}

		public List<BaseConceptProperty> getProperties() {
			return myProperties;
		}

		public void setProperties(List<BaseConceptProperty> theProperties) {
			myProperties = theProperties;
		}

		public IssueSeverity getSeverity() {
			return mySeverity;
		}

		public CodeValidationResult setSeverity(IssueSeverity theSeverity) {
			mySeverity = theSeverity;
			return this;
		}

		public List<CodeValidationIssue> getCodeValidationIssues() {
			if (myCodeValidationIssues == null) {
				myCodeValidationIssues = new ArrayList<>();
			}
			return myCodeValidationIssues;
		}

		public CodeValidationResult setCodeValidationIssues(List<CodeValidationIssue> theCodeValidationIssues) {
			myCodeValidationIssues = new ArrayList<>(theCodeValidationIssues);
			return this;
		}

		public CodeValidationResult addCodeValidationIssue(CodeValidationIssue theCodeValidationIssue) {
			getCodeValidationIssues().add(theCodeValidationIssue);
			return this;
		}

		public boolean isOk() {
			return isNotBlank(myCode);
		}

		public LookupCodeResult asLookupCodeResult(String theSearchedForSystem, String theSearchedForCode) {
			LookupCodeResult retVal = new LookupCodeResult();
			retVal.setSearchedForSystem(theSearchedForSystem);
			retVal.setSearchedForCode(theSearchedForCode);
			if (isOk()) {
				retVal.setFound(true);
				retVal.setCodeDisplay(myDisplay);
				retVal.setCodeSystemDisplayName(getCodeSystemName());
				retVal.setCodeSystemVersion(getCodeSystemVersion());
			}
			return retVal;
		}

		/**
		 * Convenience method that returns {@link #getSeverity()} as an IssueSeverity code string
		 */
		public String getSeverityCode() {
			String retVal = null;
			if (getSeverity() != null) {
				retVal = getSeverity().name().toLowerCase();
			}
			return retVal;
		}

		/**
		 * Sets an issue severity as a string code. Value must be the name of
		 * one of the enum values in {@link IssueSeverity}. Value is case-insensitive.
		 */
		public CodeValidationResult setSeverityCode(@Nonnull String theIssueSeverity) {
			setSeverity(IssueSeverity.valueOf(theIssueSeverity.toUpperCase()));
			return this;
		}

		public IBaseParameters toParameters(FhirContext theContext) {
			IBaseParameters retVal = ParametersUtil.newInstance(theContext);

			ParametersUtil.addParameterToParametersBoolean(theContext, retVal, RESULT, isOk());
			if (isNotBlank(getMessage())) {
				ParametersUtil.addParameterToParametersString(theContext, retVal, MESSAGE, getMessage());
			}
			if (isNotBlank(getDisplay())) {
				ParametersUtil.addParameterToParametersString(theContext, retVal, DISPLAY, getDisplay());
			}
			if (isNotBlank(getSourceDetails())) {
				ParametersUtil.addParameterToParametersString(theContext, retVal, SOURCE_DETAILS, getSourceDetails());
			}

			return retVal;
		}
	}

	class ValueSetExpansionOutcome {

		private final IBaseResource myValueSet;
		private final String myError;

		private boolean myErrorIsFromServer;

		public ValueSetExpansionOutcome(String theError, boolean theErrorIsFromServer) {
			myValueSet = null;
			myError = theError;
			myErrorIsFromServer = theErrorIsFromServer;
		}

		public ValueSetExpansionOutcome(IBaseResource theValueSet) {
			myValueSet = theValueSet;
			myError = null;
			myErrorIsFromServer = false;
		}

		public String getError() {
			return myError;
		}

		public IBaseResource getValueSet() {
			return myValueSet;
		}

		public boolean getErrorIsFromServer() {
			return myErrorIsFromServer;
		}
	}

	class LookupCodeResult {

		private String myCodeDisplay;
		private boolean myCodeIsAbstract;
		private String myCodeSystemDisplayName;
		private String myCodeSystemVersion;
		private boolean myFound;
		private String mySearchedForCode;
		private String mySearchedForSystem;
		private List<BaseConceptProperty> myProperties;
		private List<ConceptDesignation> myDesignations;
		private String myErrorMessage;

		/**
		 * Constructor
		 */
		public LookupCodeResult() {
			super();
		}

		public List<BaseConceptProperty> getProperties() {
			if (myProperties == null) {
				myProperties = new ArrayList<>();
			}
			return myProperties;
		}

		public void setProperties(List<BaseConceptProperty> theProperties) {
			myProperties = theProperties;
		}

		@Nonnull
		public List<ConceptDesignation> getDesignations() {
			if (myDesignations == null) {
				myDesignations = new ArrayList<>();
			}
			return myDesignations;
		}

		public String getCodeDisplay() {
			return myCodeDisplay;
		}

		public void setCodeDisplay(String theCodeDisplay) {
			myCodeDisplay = theCodeDisplay;
		}

		public String getCodeSystemDisplayName() {
			return myCodeSystemDisplayName;
		}

		public void setCodeSystemDisplayName(String theCodeSystemDisplayName) {
			myCodeSystemDisplayName = theCodeSystemDisplayName;
		}

		public String getCodeSystemVersion() {
			return myCodeSystemVersion;
		}

		public void setCodeSystemVersion(String theCodeSystemVersion) {
			myCodeSystemVersion = theCodeSystemVersion;
		}

		public String getSearchedForCode() {
			return mySearchedForCode;
		}

		public LookupCodeResult setSearchedForCode(String theSearchedForCode) {
			mySearchedForCode = theSearchedForCode;
			return this;
		}

		public String getSearchedForSystem() {
			return mySearchedForSystem;
		}

		public LookupCodeResult setSearchedForSystem(String theSearchedForSystem) {
			mySearchedForSystem = theSearchedForSystem;
			return this;
		}

		public boolean isCodeIsAbstract() {
			return myCodeIsAbstract;
		}

		public void setCodeIsAbstract(boolean theCodeIsAbstract) {
			myCodeIsAbstract = theCodeIsAbstract;
		}

		public boolean isFound() {
			return myFound;
		}

		public LookupCodeResult setFound(boolean theFound) {
			myFound = theFound;
			return this;
		}

		public void throwNotFoundIfAppropriate() {
			if (isFound() == false) {
				throw new ResourceNotFoundException(Msg.code(1738) + "Unable to find code[" + getSearchedForCode()
						+ "] in system[" + getSearchedForSystem() + "]");
			}
		}

		/**
		 * Converts the current LookupCodeResult instance into a IBaseParameters instance which is returned
		 * to the client of the $lookup operation.
		 * @param theContext the FHIR context used for running the operation
		 * @param thePropertyNamesToFilter the properties which are passed as parameter to filter the result.
		 * @return the output for the lookup operation.
		 */
		public IBaseParameters toParameters(
				FhirContext theContext, List<? extends IPrimitiveType<String>> thePropertyNamesToFilter) {

			IBaseParameters retVal = ParametersUtil.newInstance(theContext);
			if (isNotBlank(getCodeSystemDisplayName())) {
				ParametersUtil.addParameterToParametersString(theContext, retVal, "name", getCodeSystemDisplayName());
			}
			if (isNotBlank(getCodeSystemVersion())) {
				ParametersUtil.addParameterToParametersString(theContext, retVal, "version", getCodeSystemVersion());
			}
			ParametersUtil.addParameterToParametersString(theContext, retVal, "display", getCodeDisplay());
			ParametersUtil.addParameterToParametersBoolean(theContext, retVal, "abstract", isCodeIsAbstract());

			if (myProperties != null) {

				final List<BaseConceptProperty> propertiesToReturn;
				if (thePropertyNamesToFilter != null && !thePropertyNamesToFilter.isEmpty()) {
					// TODO MM: The logic to filter of properties could actually be moved to the lookupCode provider.
					// That is where the rest of the lookupCode input parameter handling is done.
					// This was left as is for now but can be done with next opportunity.
					Set<String> propertyNameList = thePropertyNamesToFilter.stream()
							.map(IPrimitiveType::getValueAsString)
							.collect(Collectors.toSet());
					propertiesToReturn = myProperties.stream()
							.filter(p -> propertyNameList.contains(p.getPropertyName()))
							.collect(Collectors.toList());
				} else {
					propertiesToReturn = myProperties;
				}

				for (BaseConceptProperty next : propertiesToReturn) {
					IBase property = ParametersUtil.addParameterToParameters(theContext, retVal, "property");
					populateProperty(theContext, property, next);
				}
			}

			if (myDesignations != null) {
				for (ConceptDesignation next : myDesignations) {
					IBase property = ParametersUtil.addParameterToParameters(theContext, retVal, "designation");
					ParametersUtil.addPartCode(theContext, property, "language", next.getLanguage());
					ParametersUtil.addPartCoding(
							theContext, property, "use", next.getUseSystem(), next.getUseCode(), next.getUseDisplay());
					ParametersUtil.addPartString(theContext, property, "value", next.getValue());
				}
			}

			return retVal;
		}

		private void populateProperty(
				FhirContext theContext, IBase theProperty, BaseConceptProperty theConceptProperty) {
			ParametersUtil.addPartCode(theContext, theProperty, "code", theConceptProperty.getPropertyName());
			String propertyType = theConceptProperty.getType();
			switch (propertyType) {
				case TYPE_STRING:
					StringConceptProperty stringConceptProperty = (StringConceptProperty) theConceptProperty;
					ParametersUtil.addPartString(theContext, theProperty, "value", stringConceptProperty.getValue());
					break;
				case TYPE_CODING:
					CodingConceptProperty codingConceptProperty = (CodingConceptProperty) theConceptProperty;
					ParametersUtil.addPartCoding(
							theContext,
							theProperty,
							"value",
							codingConceptProperty.getCodeSystem(),
							codingConceptProperty.getCode(),
							codingConceptProperty.getDisplay());
					break;
				case TYPE_GROUP:
					GroupConceptProperty groupConceptProperty = (GroupConceptProperty) theConceptProperty;
					if (groupConceptProperty.getSubProperties().isEmpty()) {
						break;
					}
					groupConceptProperty.getSubProperties().forEach(p -> {
						IBase subProperty = ParametersUtil.addPart(theContext, theProperty, "subproperty", null);
						populateProperty(theContext, subProperty, p);
					});
					break;
				default:
					throw new IllegalStateException(
							Msg.code(1739) + "Don't know how to handle " + theConceptProperty.getClass());
			}
		}

		public void setErrorMessage(String theErrorMessage) {
			myErrorMessage = theErrorMessage;
		}

		public String getErrorMessage() {
			return myErrorMessage;
		}

		public static LookupCodeResult notFound(String theSearchedForSystem, String theSearchedForCode) {
			return new LookupCodeResult()
					.setFound(false)
					.setSearchedForSystem(theSearchedForSystem)
					.setSearchedForCode(theSearchedForCode);
		}
	}

	class TranslateCodeRequest {
		private final String myTargetSystemUrl;
		private final String myConceptMapUrl;
		private final String myConceptMapVersion;
		private final String mySourceValueSetUrl;
		private final String myTargetValueSetUrl;
		private final IIdType myResourceId;
		private final boolean myReverse;
		private List<IBaseCoding> myCodings;

		public TranslateCodeRequest(List<IBaseCoding> theCodings, String theTargetSystemUrl) {
			myCodings = theCodings;
			myTargetSystemUrl = theTargetSystemUrl;
			myConceptMapUrl = null;
			myConceptMapVersion = null;
			mySourceValueSetUrl = null;
			myTargetValueSetUrl = null;
			myResourceId = null;
			myReverse = false;
		}

		public TranslateCodeRequest(
				List<IBaseCoding> theCodings,
				String theTargetSystemUrl,
				String theConceptMapUrl,
				String theConceptMapVersion,
				String theSourceValueSetUrl,
				String theTargetValueSetUrl,
				IIdType theResourceId,
				boolean theReverse) {
			myCodings = theCodings;
			myTargetSystemUrl = theTargetSystemUrl;
			myConceptMapUrl = theConceptMapUrl;
			myConceptMapVersion = theConceptMapVersion;
			mySourceValueSetUrl = theSourceValueSetUrl;
			myTargetValueSetUrl = theTargetValueSetUrl;
			myResourceId = theResourceId;
			myReverse = theReverse;
		}

		@Override
		public boolean equals(Object theO) {
			if (this == theO) {
				return true;
			}

			if (theO == null || getClass() != theO.getClass()) {
				return false;
			}

			TranslateCodeRequest that = (TranslateCodeRequest) theO;

			return new EqualsBuilder()
					.append(myCodings, that.myCodings)
					.append(myTargetSystemUrl, that.myTargetSystemUrl)
					.append(myConceptMapUrl, that.myConceptMapUrl)
					.append(myConceptMapVersion, that.myConceptMapVersion)
					.append(mySourceValueSetUrl, that.mySourceValueSetUrl)
					.append(myTargetValueSetUrl, that.myTargetValueSetUrl)
					.append(myResourceId, that.myResourceId)
					.append(myReverse, that.myReverse)
					.isEquals();
		}

		@Override
		public int hashCode() {
			return new HashCodeBuilder(17, 37)
					.append(myCodings)
					.append(myTargetSystemUrl)
					.append(myConceptMapUrl)
					.append(myConceptMapVersion)
					.append(mySourceValueSetUrl)
					.append(myTargetValueSetUrl)
					.append(myResourceId)
					.append(myReverse)
					.toHashCode();
		}

		public List<IBaseCoding> getCodings() {
			return myCodings;
		}

		public String getTargetSystemUrl() {
			return myTargetSystemUrl;
		}

		public String getConceptMapUrl() {
			return myConceptMapUrl;
		}

		public String getConceptMapVersion() {
			return myConceptMapVersion;
		}

		public String getSourceValueSetUrl() {
			return mySourceValueSetUrl;
		}

		public String getTargetValueSetUrl() {
			return myTargetValueSetUrl;
		}

		public IIdType getResourceId() {
			return myResourceId;
		}

		public boolean isReverse() {
			return myReverse;
		}

		@Override
		public String toString() {
			return new ToStringBuilder(this)
					.append("sourceValueSetUrl", mySourceValueSetUrl)
					.append("targetSystemUrl", myTargetSystemUrl)
					.append("targetValueSetUrl", myTargetValueSetUrl)
					.append("reverse", myReverse)
					.toString();
		}
	}

	/**
	 * <p
	 * Warning: This method's behaviour and naming is preserved for backwards compatibility, BUT the actual naming and
	 * function are not aligned.
	 * </p
	 * <p>
	 * See VersionSpecificWorkerContextWrapper#validateCode in hapi-fhir-validation, and the refer to the values below
	 * for the behaviour associated with each value.
	 * </p>
	 * <p>
	 *   <ul>
	 *     <li>If <code>false</code> (default setting) the validation for codings will return a positive result only if
	 *     ALL codings are valid.</li>
	 * 	   <li>If <code>true</code> the validation for codings will return a positive result if ANY codings are valid.
	 * 	   </li>
	 * 	  </ul>
	 * </p>
	 * @return true or false depending on the desired coding validation behaviour.
	 */
	default boolean isEnabledValidationForCodingsLogicalAnd() {
		return false;
	}
}
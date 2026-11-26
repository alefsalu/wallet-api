package com.portfolio.wallet.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.portfolio.wallet", importOptions = {ImportOption.DoNotIncludeTests.class})
public class HexagonalArchitectureTest {

    @ArchTest
    public static final ArchRule domainMustNotDependOnFrameworks =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.persistence..",
                            "jakarta.servlet..",
                            "org.hibernate..",
                            "com.portfolio.wallet.infrastructure..",
                            "com.portfolio.wallet.interfaces..");

    @ArchTest
    public static final ArchRule applicationMustNotDependOnInfrastructureOrRest =
            noClasses()
                    .that()
                    .resideInAPackage("..application..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.portfolio.wallet.infrastructure..",
                            "com.portfolio.wallet.interfaces..",
                            "jakarta.persistence..",
                            "org.hibernate..");

    @ArchTest
    public static final ArchRule domainExceptionsMustExtendDomainException =
            classes()
                    .that()
                    .resideInAPackage("..domain.exception..")
                    .and()
                    .haveSimpleNameEndingWith("Exception")
                    .should()
                    .beAssignableTo(RuntimeException.class);
}

package com.sphereon.oid.fed.server.config

import com.sphereon.crypto.kms.IKeyManagementSystem
import com.sphereon.oid.fed.logger.Logger
import com.sphereon.oid.fed.persistence.Persistence
import com.sphereon.oid.fed.services.*
import com.sphereon.oid.fed.services.config.AccountServiceConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
open class ServiceConfig {

    val logger = Logger.tag("FederationServerServiceConfig")

    @Bean
    open fun accountConfig(environment: Environment): AccountServiceConfig {
        return AccountServiceConfig(
            environment.getProperty("sphereon.federation.root-identifier", "http://localhost:8080")
        )
    }

    @Bean
    open fun logService(): LogService {
        return LogService(Persistence.logQueries)
    }

    @Bean
    open fun entityConfigurationMetadataService(): MetadataService {
        return MetadataService()
    }

    @Bean
    open fun authorityHintService(): AuthorityHintService {
        return AuthorityHintService()
    }

    @Bean
    open fun accountService(accountServiceConfig: AccountServiceConfig): AccountService {
        return AccountService(accountServiceConfig)
    }

    @Bean
    open fun kmsService(environment: Environment): KmsService {
        val providerType = environment.getProperty("sphereon.federation.service.kms.provider", "memory")

        return when (KmsType.fromString(providerType)) {
            KmsType.AWS -> {
                try {
                    KmsService.createAwsKms(
                        applicationId = environment.getProperty(
                            "sphereon.federation.aws.application-id",
                            "sphereon-federation-aws"
                        ),
                        region = environment.getRequiredProperty("sphereon.federation.aws.region"),
                        accessKeyId = environment.getRequiredProperty("sphereon.federation.aws.access-key-id"),
                        secretAccessKey = environment.getRequiredProperty("sphereon.federation.aws.secret-access-key"),
                        maxRetries = environment.getProperty(
                            "sphereon.federation.aws.max-retries",
                            Int::class.java,
                            10
                        ),
                        baseDelayInMS = environment.getProperty(
                            "sphereon.federation.aws.base-delay",
                            Long::class.java,
                            500L
                        ),
                        maxDelayInMS = environment.getProperty(
                            "sphereon.federation.aws.max-delay",
                            Long::class.java,
                            15000L
                        )
                    )
                } catch (e: Exception) {
                    logger.error("Error initializing AWS KMS provider: ${e.message}")
                    throw e
                }
            }

            KmsType.AZURE -> {
                try {
                    KmsService.createAzureKms(
                        applicationId = environment.getRequiredProperty("sphereon.federation.azure.application-id"),
                        keyvaultUrl = environment.getRequiredProperty("sphereon.federation.azure.keyvault-url"),
                        tenantId = environment.getRequiredProperty("sphereon.federation.azure.tenant-id"),
                        clientId = environment.getRequiredProperty("sphereon.federation.azure.client-id"),
                        clientSecret = environment.getRequiredProperty("sphereon.federation.azure.client-secret"),
                        maxRetries = environment.getProperty(
                            "sphereon.federation.azure.max-retries",
                            Int::class.java,
                            10
                        ),
                        baseDelayInMS = environment.getProperty(
                            "sphereon.federation.azure.base-delay",
                            Long::class.java,
                            500L
                        ),
                        maxDelayInMS = environment.getProperty(
                            "sphereon.federation.azure.max-delay",
                            Long::class.java,
                            15000L
                        )
                    )
                } catch (e: Exception) {
                    logger.error("Error initializing Azure KMS provider: ${e.message}")
                    throw e
                }
            }

            else -> KmsService.createMemoryKms()
        }
    }


    @Bean
    open fun keyManagementSystem(kmsService: KmsService): IKeyManagementSystem {
        return kmsService.getKmsProvider()
    }

    @Bean
    open fun keyService(kmsService: KmsService): JwkService {
        return JwkService(kmsService)
    }

    @Bean
    open fun jwtService(keyManagementSystem: IKeyManagementSystem): JwtService {
        return JwtService(keyManagementSystem)
    }

    @Bean
    open fun subordinateService(
        accountService: AccountService,
        jwkService: JwkService,
        keyManagementSystem: IKeyManagementSystem
    ): SubordinateService {
        return SubordinateService(accountService, jwkService, keyManagementSystem)
    }

    @Bean
    open fun trustMarkService(
        jwkService: JwkService,
        keyManagementSystem: IKeyManagementSystem,
        accountService: AccountService
    ): TrustMarkService {
        return TrustMarkService(jwkService, keyManagementSystem, accountService)
    }

    @Bean
    open fun critService(): CriticalClaimService {
        return CriticalClaimService()
    }

    @Bean
    open fun entityConfigurationStatementService(
        accountService: AccountService,
        jwkService: JwkService,
        keyManagementSystem: IKeyManagementSystem
    ): EntityConfigurationStatementService {
        return EntityConfigurationStatementService(accountService, jwkService, keyManagementSystem)
    }

    @Bean
    open fun receivedTrustMarkService(): ReceivedTrustMarkService {
        return ReceivedTrustMarkService()
    }

    @Bean
    open fun resolveService(
        accountService: AccountService,
        jwkService: JwkService,
        keyManagementSystem: IKeyManagementSystem
    ): ResolutionService {
        return ResolutionService(
            accountService,
            jwkService,
            keyManagementSystem
        )
    }
}

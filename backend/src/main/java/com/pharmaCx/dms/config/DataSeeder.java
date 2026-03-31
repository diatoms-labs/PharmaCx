package com.pharmaCx.dms.config;

import com.pharmaCx.dms.domain.enums.UserRole;
import com.pharmaCx.dms.domain.model.AppUser;
import com.pharmaCx.dms.domain.model.DocumentTypeConfig;
import com.pharmaCx.dms.domain.model.OrganizationalUnit;
import com.pharmaCx.dms.domain.model.SystemSetting;
import com.pharmaCx.dms.domain.repository.AppUserRepository;
import com.pharmaCx.dms.domain.repository.DocumentTypeConfigRepository;
import com.pharmaCx.dms.domain.repository.OrganizationalUnitRepository;
import com.pharmaCx.dms.domain.repository.SystemSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final AppUserRepository userRepo;
    private final OrganizationalUnitRepository orgUnitRepo;
    private final DocumentTypeConfigRepository docTypeRepo;
    private final SystemSettingRepository settingsRepo;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(AppUserRepository userRepo,
                      OrganizationalUnitRepository orgUnitRepo,
                      DocumentTypeConfigRepository docTypeRepo,
                      SystemSettingRepository settingsRepo,
                      PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.orgUnitRepo = orgUnitRepo;
        this.docTypeRepo = docTypeRepo;
        this.settingsRepo = settingsRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedOrganizationalUnits();
        seedDocumentTypeConfigs();
        seedUsers();
        seedSystemSettings();
    }

    // ======================== ORG UNIT SEEDING ========================

    private void seedOrganizationalUnits() {
        if (orgUnitRepo.count() > 0) {
            log.info("Organizational units already exist, skipping seed");
            return;
        }

        log.info("Seeding organizational units...");
        Instant now = Instant.now();

        List<OrganizationalUnit> units = List.of(
                buildUnit("QA",   "Quality Assurance",        "DEPARTMENT", now),
                buildUnit("PROD", "Production",                "DEPARTMENT", now),
                buildUnit("RD",   "Research and Development",  "DEPARTMENT", now),
                buildUnit("QC",   "Quality Control",           "DEPARTMENT", now),
                buildUnit("RA",   "Regulatory Affairs",        "DEPARTMENT", now),
                buildUnit("SC",   "Supply Chain",              "DEPARTMENT", now),
                buildUnit("EM",   "Engineering & Maintenance", "DEPARTMENT", now),
                buildUnit("ADM",  "Administration",            "DEPARTMENT", now)
        );

        orgUnitRepo.saveAll(units);
        log.info("Seeded {} organizational units", orgUnitRepo.count());
    }

    private OrganizationalUnit buildUnit(String code, String displayName, String type, Instant now) {
        OrganizationalUnit u = new OrganizationalUnit();
        u.setCode(code);
        u.setDisplayName(displayName);
        u.setType(type);
        u.setActive(true);
        u.setCreatedAt(now);
        u.setUpdatedAt(now);
        return u;
    }

    // ======================== DOC TYPE SEEDING ========================

    private void seedDocumentTypeConfigs() {
        if (docTypeRepo.count() > 0) {
            log.info("Document type configs already exist, skipping seed");
            return;
        }

        log.info("Seeding document type configs...");
        Instant now = Instant.now();

        // Look up the QA unit to set as default owner for most doc types
        String qaUnitId = orgUnitRepo.findByCode("QA").map(OrganizationalUnit::getId).orElse(null);

        List<DocumentTypeConfig> types = List.of(
                buildDocType("SOP",  "Standard Operating Procedure", "SOP", qaUnitId, now),
                buildDocType("WI",   "Work Instruction",             "WI",  qaUnitId, now),
                buildDocType("FORM", "Form",                         "FRM", qaUnitId, now),
                buildDocType("POL",  "Policy",                       "POL", qaUnitId, now),
                buildDocType("SPEC", "Specification",                "SPE", qaUnitId, now),
                buildDocType("VAL",  "Validation Protocol",          "VAL", qaUnitId, now)
        );

        docTypeRepo.saveAll(types);
        log.info("Seeded {} document type configs", docTypeRepo.count());
    }

    private DocumentTypeConfig buildDocType(String code, String displayName, String prefix,
                                             String ownerUnitId, Instant now) {
        DocumentTypeConfig dt = new DocumentTypeConfig();
        dt.setCode(code);
        dt.setDisplayName(displayName);
        dt.setNumberingPrefix(prefix);
        dt.setOwnerUnitId(ownerUnitId);
        dt.setActive(true);
        dt.setCreatedAt(now);
        dt.setUpdatedAt(now);
        return dt;
    }

    // ======================== USER SEEDING ========================

    private void seedUsers() {
        if (userRepo.count() > 0) {
            log.info("Users already exist, skipping user seed data");
            return;
        }

        // Build a code -> id lookup map for org units
        Map<String, String> unitIdByCode = orgUnitRepo.findAll().stream()
                .collect(Collectors.toMap(OrganizationalUnit::getCode, OrganizationalUnit::getId));

        log.info("Seeding default users...");
        String defaultPassword = passwordEncoder.encode("password123");

        // SYSTEM_ADMIN (Fixed ID for session persistence across rebuilds)
        createUserWithId("admin-rajesh-kumar-001", "rajesh.kumar", "rajesh.kumar@pharmaCx.com", "Dr. Rajesh Kumar", defaultPassword,
                UserRole.SYSTEM_ADMIN, unitIdByCode.get("ADM"));

        // DIRECTOR
        createUser("anita.desai", "anita.desai@pharmaCx.com", "Dr. Anita Desai", defaultPassword,
                UserRole.DIRECTOR, unitIdByCode.get("QA"));
        createUser("vikram.mehta", "vikram.mehta@pharmaCx.com", "Dr. Vikram Mehta", defaultPassword,
                UserRole.DIRECTOR, unitIdByCode.get("PROD"));

        // HEAD_OF_DEPARTMENT
        createUser("sunita.sharma", "sunita.sharma@pharmaCx.com", "Sunita Sharma", defaultPassword,
                UserRole.HEAD_OF_DEPARTMENT, unitIdByCode.get("QA"));
        createUser("arjun.reddy", "arjun.reddy@pharmaCx.com", "Arjun Reddy", defaultPassword,
                UserRole.HEAD_OF_DEPARTMENT, unitIdByCode.get("PROD"));
        createUser("kavitha.nair", "kavitha.nair@pharmaCx.com", "Dr. Kavitha Nair", defaultPassword,
                UserRole.HEAD_OF_DEPARTMENT, unitIdByCode.get("RD"));
        createUser("manoj.gupta", "manoj.gupta@pharmaCx.com", "Manoj Gupta", defaultPassword,
                UserRole.HEAD_OF_DEPARTMENT, unitIdByCode.get("QC"));

        // MANAGER
        createUser("deepa.iyer", "deepa.iyer@pharmaCx.com", "Deepa Iyer", defaultPassword,
                UserRole.MANAGER, unitIdByCode.get("QA"));
        createUser("suresh.patel", "suresh.patel@pharmaCx.com", "Suresh Patel", defaultPassword,
                UserRole.MANAGER, unitIdByCode.get("PROD"));
        createUser("priya.menon", "priya.menon@pharmaCx.com", "Priya Menon", defaultPassword,
                UserRole.MANAGER, unitIdByCode.get("RD"));
        createUser("amanda.foster", "amanda.foster@pharmaCx.com", "Amanda Foster", defaultPassword,
                UserRole.MANAGER, unitIdByCode.get("RD"));

        // ENGINEER
        createUser("rahul.verma", "rahul.verma@pharmaCx.com", "Rahul Verma", defaultPassword,
                UserRole.ENGINEER, unitIdByCode.get("QA"));
        createUser("neha.joshi", "neha.joshi@pharmaCx.com", "Neha Joshi", defaultPassword,
                UserRole.ENGINEER, unitIdByCode.get("QA"));
        createUser("amit.singh", "amit.singh@pharmaCx.com", "Amit Singh", defaultPassword,
                UserRole.ENGINEER, unitIdByCode.get("PROD"));
        createUser("chris.martinez", "chris.martinez@pharmaCx.com", "Chris Martinez", defaultPassword,
                UserRole.ENGINEER, unitIdByCode.get("PROD"));
        createUser("pooja.rao", "pooja.rao@pharmaCx.com", "Pooja Rao", defaultPassword,
                UserRole.ENGINEER, unitIdByCode.get("RD"));
        createUser("karthik.sundaram", "karthik.sundaram@pharmaCx.com", "Karthik Sundaram", defaultPassword,
                UserRole.ENGINEER, unitIdByCode.get("QC"));
        createUser("andrew.kim", "andrew.kim@pharmaCx.com", "Andrew Kim", defaultPassword,
                UserRole.ENGINEER, unitIdByCode.get("QC"));
        createUser("divya.pillai", "divya.pillai@pharmaCx.com", "Divya Pillai", defaultPassword,
                UserRole.ENGINEER, unitIdByCode.get("QA"));

        // OPERATOR
        createUser("ravi.shankar", "ravi.shankar@pharmaCx.com", "Ravi Shankar", defaultPassword,
                UserRole.OPERATOR, unitIdByCode.get("PROD"));
        createUser("lakshmi.devi", "lakshmi.devi@pharmaCx.com", "Lakshmi Devi", defaultPassword,
                UserRole.OPERATOR, unitIdByCode.get("PROD"));
        createUser("ganesh.murugan", "ganesh.murugan@pharmaCx.com", "Ganesh Murugan", defaultPassword,
                UserRole.OPERATOR, unitIdByCode.get("QC"));

        log.info("Seeded {} users", userRepo.count());
    }

    private void seedSystemSettings() {
        if (settingsRepo.findByScopeAndScopeIdIsNull("GLOBAL").isPresent()) {
            log.info("Global system settings already exist, skipping seed");
            return;
        }

        log.info("Seeding global system settings...");
        SystemSetting global = new SystemSetting();
        global.setScope("GLOBAL");
        global.setScopeId(null);
        global.setUpdatedAt(Instant.now());
        global.setUpdatedBy("SYSTEM");
        
        SystemSetting.SettingValues v = global.getSettings();
        v.setAiStrategy("LOCAL");
        v.setCloudAiProvider("GOOGLE");
        v.setCloudAiModel("gemini-1.5-flash");
        
        settingsRepo.save(global);
        log.info("Seeded global system settings");
    }

    private void createUser(String username, String email, String fullName, String passwordHash,
                            UserRole role, String unitId) {
        createUserWithId(null, username, email, fullName, passwordHash, role, unitId);
    }

    private void createUserWithId(String id, String username, String email, String fullName, String passwordHash,
                            UserRole role, String unitId) {
        AppUser user = new AppUser();
        if (id != null) user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPasswordHash(passwordHash);
        user.setRole(role);
        user.setUnitId(unitId);
        user.setActive(true);
        user.setMustChangePassword(false);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        userRepo.save(user);
    }
}

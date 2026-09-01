package com.simba.snip.npo.api;

import com.simba.snip.npo.network.NetworkDomainService;
import com.simba.snip.npo.persist.SiteEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SiteController {

    private final NetworkDomainService domainService;

    public SiteController(NetworkDomainService domainService) {
        this.domainService = domainService;
    }

    @GetMapping("/api/v1/sites")
    public List<SiteDto> list() {
        return domainService.listSites().stream().map(SiteController::toDto).toList();
    }

    @GetMapping("/api/v1/sites/{siteId}")
    public SiteDto get(@PathVariable String siteId) {
        return toDto(domainService.requireSite(siteId));
    }

    static SiteDto toDto(SiteEntity site) {
        return new SiteDto(site.getSiteId(), site.getName(), site.getLatitude(), site.getLongitude(), site.getStatus());
    }
}

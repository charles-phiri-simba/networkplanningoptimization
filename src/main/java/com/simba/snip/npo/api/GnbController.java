package com.simba.snip.npo.api;

import com.simba.snip.npo.network.NetworkDomainService;
import com.simba.snip.npo.persist.GnbEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class GnbController {

    private final NetworkDomainService domainService;

    public GnbController(NetworkDomainService domainService) {
        this.domainService = domainService;
    }

    @GetMapping("/api/v1/gnbs")
    public List<GnbDto> list() {
        return domainService.listGnbs().stream().map(GnbController::toDto).toList();
    }

    @GetMapping("/api/v1/gnbs/{gnbId}")
    public GnbDto get(@PathVariable String gnbId) {
        return toDto(domainService.requireGnb(gnbId));
    }

    static GnbDto toDto(GnbEntity gnb) {
        return new GnbDto(
                gnb.getGnbId(),
                gnb.getName(),
                gnb.getSite().getSiteId(),
                gnb.getVendor(),
                gnb.getModel(),
                gnb.getStatus()
        );
    }
}

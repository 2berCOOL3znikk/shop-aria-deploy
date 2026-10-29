package com.aria.service.integrations;
import java.util.Map;
/** A future approved API adapter must feed the same matching/constraint boundary as CSV. */
public interface CompetitorImportAdapter {
 record Snapshot(Long sourceId,String csv,boolean identityConfirmed){}
 Map<String,Object> importSnapshot(Snapshot snapshot);
}

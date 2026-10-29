package com.aria.service.integrations;
import com.aria.service.CompetitorPriceService;
import org.springframework.stereotype.Component;
import java.util.Map;
@Component
public class CsvCompetitorAdapter implements CompetitorImportAdapter {
 private final CompetitorPriceService prices;
 public CsvCompetitorAdapter(CompetitorPriceService p){prices=p;}
 @Override public Map<String,Object> importSnapshot(Snapshot s){return prices.importCsv(s.sourceId(),s.csv(),s.identityConfirmed());}
}

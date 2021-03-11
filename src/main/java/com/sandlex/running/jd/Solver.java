package com.sandlex.running.jd;

import com.sandlex.running.jd.model.Calculable;
import com.sandlex.running.jd.model.Distance;
import com.sandlex.running.jd.model.Duration;
import com.sandlex.running.jd.model.Measure;
import com.sandlex.running.jd.model.Pace;
import com.sandlex.running.jd.model.PaceBlock;
import com.sandlex.running.jd.model.PaceName;
import com.sandlex.running.jd.model.PaceValue;
import com.sandlex.running.jd.model.Phase;
import com.sandlex.running.jd.model.Repetition;
import com.sandlex.running.jd.model.Schema;
import lombok.experimental.UtilityClass;

import java.util.Map;

import static java.util.stream.Collectors.toMap;

@UtilityClass
class Solver {

    Estimation solve(PaceBlock paceBlock, Schema schema, Distance.System system) {
        Map<PaceName, PaceValue> paces = paceBlock.getPaces().stream().collect(toMap(Pace::getPaceName, Pace::getPaceValue));

        schema.getPhases().forEach(calculable -> Solver.validate(paces, calculable));

        Estimation estimation = new Estimation();
        schema.getPhases().forEach(calculable -> Solver.calculate(paces, calculable, estimation));

        return estimation;
    }

    private void validate(Map<PaceName, PaceValue> paces, Calculable calculable) {
        if (calculable instanceof Phase) {
            Phase phase = (Phase) calculable;
            if (!paces.containsKey(phase.getPaceName())) {
                throw new IllegalArgumentException("Unknown pace: " + phase.getPaceName());
            }
        } else if (calculable instanceof Repetition) {
            Repetition repetition = (Repetition) calculable;
            repetition.getPhases().forEach(rep -> Solver.validate(paces, rep));
        }
    }

    private void calculate(Map<PaceName, PaceValue> paces, Calculable calculable, Estimation estimation) {
        if (calculable instanceof Phase) {
            Phase phase = (Phase) calculable;
            Measure measure = phase.getMeasure();
            if (measure instanceof Distance) {
                double kilometers = ((Distance) measure).getKilometers(Distance.System.METRIC);
                estimation.addKilometers(kilometers);
                PaceValue paceValue = paces.get(phase.getPaceName());
                estimation.addSeconds(Math.round(kilometers * paceValue.getInSeconds()));
            } else if (measure instanceof Duration) {
                int seconds = ((Duration) measure).getInSeconds();
                estimation.addSeconds(seconds);
                PaceValue paceValue = paces.get(phase.getPaceName());
                estimation.addKilometers(seconds / paceValue.getInSeconds());
            }
        } else if (calculable instanceof Repetition) {
            Repetition repetition = (Repetition) calculable;
            Estimation repetitionEstimation = new Estimation();
            repetition.getPhases().forEach(rep -> Solver.calculate(paces, rep, repetitionEstimation));
            estimation.addKilometers(repetitionEstimation.getKilometers() * repetition.getRepetitionCount().getValue());
            estimation.addSeconds(repetitionEstimation.getSeconds() * repetition.getRepetitionCount().getValue());
        }
    }

}

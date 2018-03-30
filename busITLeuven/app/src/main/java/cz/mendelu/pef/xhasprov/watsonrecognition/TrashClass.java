package cz.mendelu.pef.xhasprov.watsonrecognition;

/**
 * Created by krist on 29. 3. 2018.
 */

public class TrashClass {

    String className;
    Double score;

    public TrashClass(String className, Double score) {
        this.className = className;
        this.score = score;
    }

    public TrashClass() {
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    @Override
    public String toString() {
        return "TrashClass{" +
                "className='" + className + '\'' +
                ", score=" + score +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TrashClass that = (TrashClass) o;

        if (className != null ? !className.equals(that.className) : that.className != null)
            return false;
        return score != null ? score.equals(that.score) : that.score == null;
    }

    @Override
    public int hashCode() {
        int result = className != null ? className.hashCode() : 0;
        result = 31 * result + (score != null ? score.hashCode() : 0);
        return result;
    }
}

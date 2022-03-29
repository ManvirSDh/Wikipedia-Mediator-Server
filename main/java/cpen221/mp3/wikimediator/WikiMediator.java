package cpen221.mp3.wikimediator;

import cpen221.mp3.fsftbuffer.BufferableWikiPage;
import cpen221.mp3.fsftbuffer.FSFTBuffer;
import cpen221.mp3.fsftbuffer.ObjectNotFoundException;
import org.fastily.jwiki.core.Wiki;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/*
 * Representation Invariants (RI):
 * Times added to pastSearchGetRequests are a subset of times added to requestTimes.
 *
 * Abstraction Function (AF):
 * WikiMediator gets information from Wikipedia using the JWiki API and responds to
 * requests from user with information from Wikipedia.
 * 
 * pageBuffer represents a cache of pages that have been requested, to minimize
 * internet requests.
 * wiki represents a connection to Wikipedia, and allows WikiMediator to access
 * information from Wikipedia.
 * requestTimes holds all the times at which WikiMediator has received a request.
 * pastSearchGetRequests holds a list of all past search/getPage requests, and the times
 * each was received.
 */
public class WikiMediator {

    public static final long SECONDS_TO_MILLIS = 1000;

    private final FSFTBuffer<BufferableWikiPage> pageBuffer;
    Wiki wiki;
    Map<String, List<Long>> pastSearchGetRequests;
    List<Long> requestTimes;

    /**
     * Creates a WikiMediator that accesses Wikipedia through an APY, and stores
     * pages in a local cache.
     * @param capacity     maximum number of pages that can be stored in
     *                     local buffer.
     *                     capacity >= 0
     * @param stalenessInterval amount of time, in seconds, that pages are
     *                          stored in buffer before being erased.
     *                          stalenessInterval >= 0
     */
    public WikiMediator(int capacity, int stalenessInterval) {
        pageBuffer = new FSFTBuffer(capacity, stalenessInterval);
        wiki = new Wiki.Builder().withDomain("en.wikipedia.org").build();
        pastSearchGetRequests = Collections.synchronizedMap(new HashMap<>());
        requestTimes = Collections.synchronizedList(new ArrayList<>());
    }

    /**
     * checks that the rep invariant of the object is maintained.
     */
    private void checkRep() {
        Set<Long> searchGetTimeSet = new HashSet<>();
        pastSearchGetRequests.forEach((s, tl) -> {
            searchGetTimeSet.addAll(tl);
        });

        assert requestTimes.containsAll(searchGetTimeSet);
    }

    /**
     * Searches Wikipedia and returns a list of possible results.
     * @param query the query that should be searched.
     *              query != null
     * @param limit number of results to return. Input -1 for unlimited results
     *              limt >= -1
     * @return  a list of the results received from Wikipedia when the query
     *          is searched.
     */
    public List<String> search(String query, int limit) {
        Long time = System.currentTimeMillis();
        requestTimes.add(time);

        if (!pastSearchGetRequests.containsKey(query)) {
            pastSearchGetRequests.put(query, new ArrayList<Long>());
        }
        pastSearchGetRequests.get(query).add(time);

        checkRep();
        return wiki.search(query, limit);
    }

    /**
     * Searches Wikipedia and returns the page asked for.
     * @param pageTitle the title of the page that is asked for.
     *                  pageTitle != null
     * @return  The full text of the Wikipedia page corresponding to the input.
     */
    public String getPage(String pageTitle) {
        Long time = System.currentTimeMillis();
        requestTimes.add(time);

        if (!pastSearchGetRequests.containsKey(pageTitle)) {
            pastSearchGetRequests.put(pageTitle, new ArrayList<Long>());
        }
        pastSearchGetRequests.get(pageTitle).add(time);

        try {
            if (pageBuffer.touch(pageTitle)) {
                return pageBuffer.get(pageTitle).getPageText();
            }
        } catch (ObjectNotFoundException e) {
            e.printStackTrace();
        }

        String pageText = wiki.getPageText(pageTitle);
        BufferableWikiPage newPage = new BufferableWikiPage(pageTitle, pageText);
        pageBuffer.put(newPage);

        checkRep();
        return pageText;
    }

    /**
     * Returns a list of the most popular search/getPage requests sent to
     * this WikiMediator, ranked in order from most popular to least.
     * @param limit limit on the length of the list returned
     *              limit > 0
     * @return  A list of the most popular search/getPage requests sent
     *             to this WikiMediator, ranked in order from most popular to least.
     */
    public List<String> zeitgeist(int limit) {
        Long time = System.currentTimeMillis();
        requestTimes.add(time);
        ArrayList<String> rankedSearches = new ArrayList<>();
        for (Map.Entry<String, List<Long>> search : pastSearchGetRequests.entrySet()) {
            if (rankedSearches.size() == 0) {
                rankedSearches.add(search.getKey());
            } else {
                for (int i = 0; i < rankedSearches.size(); i++) {
                    if (search.getValue().size() > pastSearchGetRequests.get(rankedSearches.get(i)).size()) {
                        rankedSearches.add(i, search.getKey());
                        break;
                    }
                }
                if (!rankedSearches.contains(search.getKey())) {
                    rankedSearches.add(search.getKey());
                }
            }
        }

        if (limit > rankedSearches.size()) {
            return rankedSearches;
        } else  {
            return rankedSearches.subList(0, limit - 1);
        }
    }

    /**
     * Returns a list of the most popular search/getPage requests sent to
     * this WikiMediator in the last while, defined by input, ranked in order
     * from most popular to least.
     * @param timeLimitInSeconds    any requests older than timeLimitInSeconds will
     *                              be ignored when determining popularity of
     *                              requests
     * @param maxItems              max length of the list of requests returned
     *                              maxItems > 0
     * @return  A list of the most popular search/getPage requests, in time window
     *          current time - timeLimitInSeconds to current time. Ranked in order
     *          from most popular to least popular.
     */
    public List<String> trending(int timeLimitInSeconds, int maxItems) {
        Long time = System.currentTimeMillis();
        requestTimes.add(time);
        Long timeLimitInMS = timeLimitInSeconds * SECONDS_TO_MILLIS;
        ArrayList<String> rankedSearches = new ArrayList<>();
        Map<String, List<Long>> recentSearches = new HashMap<>(pastSearchGetRequests);

        recentSearches.replaceAll(
                (s, v) -> recentSearches.get(s).stream()
                        .filter(_time -> time - _time <= timeLimitInMS)
                        .collect(Collectors.toList()));

        for (Map.Entry<String, List<Long>> search : recentSearches.entrySet()) {
            if (rankedSearches.size() == 0) {
                rankedSearches.add(search.getKey());
            } else {
                for (int i = 0; i < rankedSearches.size(); i++) {
                    if (search.getValue().size() > recentSearches.get(rankedSearches.get(i)).size()) {
                        rankedSearches.add(i, search.getKey());
                        break;
                    }
                }
                if (!rankedSearches.contains(search.getKey())) {
                    rankedSearches.add(search.getKey());
                }
            }
        }

        if (maxItems > rankedSearches.size()) {
            return rankedSearches;
        } else {
            return rankedSearches.subList(0, maxItems - 1);
        }
    }

    /**
     * Finds the maximum number of requests received in any time window of given length.
     * Includes all requests made to methods of WikiMediator, including the one done to
     * use this function.
     * @param timeWindowInSeconds   length of time window whose max number of requests should
     *                              be found. time window is similar to a sliding window, and
     *                              any time window is valid as long as the length is equal to
     *                              timeWindowInSeconds
     * @return the maximum number received in any time window of given length timeWindowInSeconds
     */
    public int windowedPeakLoad(int timeWindowInSeconds) {
        Long time = System.currentTimeMillis();
        requestTimes.add(time);

        requestTimes.sort(Comparator.naturalOrder());

        Long timeIntervalInMS = timeWindowInSeconds * SECONDS_TO_MILLIS;
        int endOfInterval = 0;
        int maxCount = 0;
        int tempCount;

        for (int i = 0; i < requestTimes.size(); i++) {
            for (int j = endOfInterval + 1; j < requestTimes.size(); j++) {
                if (requestTimes.get(j) - requestTimes.get(i) <= timeIntervalInMS) {
                    endOfInterval = j;
                } else {
                    break;
                }
            }
            tempCount = endOfInterval - i;
            if (tempCount >  maxCount) {
                maxCount = tempCount;
            }
        }

        return maxCount;
    }

    /**
     * Finds the maximum number of requests received in any 30 second interval. This
     * interval is similar to a sliding window, as any time window of 30 seconds is
     * valid.
     * Includes all requests made to methods of WikiMediator, including the one done to
     * use this function.
     * @return the maximum number of requests received in any 30 second interval.
     */
    public int windowedPeakLoad() {
        return windowedPeakLoad(30);
    }

    /**
     * Given two wikipedia pages, find the smallest path between the two pages.
     * Equivalent to the minimum pages required to travel through if you
     * were trying to traverse from one page to another using only hyperlinks
     * on that page.
     * If there are two or more paths, return the lexicographically smaller
     * path.
     * @param pageTitle1    Page that the process should start on
     *                      pageTitle1 != null
     * @param pageTitle2    Page that the process should end on
     *                      pageTitle2 !- null
     * @param timeout       Amount of time given to search for this path, in seconds.
     *                      Throw TimeoutException if not found before timeout seconds
     *                      elapsed
     *                      timeout >= 0
     * @return              The shortest path between two given wikipedia pages.
     *                      If there's a tie, the lexicographically smallest one is
     *                      chosen.
     * @throws TimeoutException Thrown if searching takes more than timout seconds.
     */
    public List<String> shortestPath(String pageTitle1, String pageTitle2, int timeout) throws TimeoutException {
        Long time = System.currentTimeMillis();
        requestTimes.add(time);
        ExecutorService timer = Executors.newSingleThreadExecutor();
        AtomicBoolean exit = new AtomicBoolean(false);
        Future<ArrayList<String>> future = timer.submit(() -> {
            final boolean[] found = {false};
            Set<ArrayList<String>> pathList = new HashSet<>();
            Set<String> previousNodes = Collections.synchronizedSet(new HashSet<>());
            previousNodes.add(pageTitle1);

            List<String> linkListTemp = wiki.getLinksOnPage(pageTitle1);
            for (String link : linkListTemp) {
                if (link.equals(pageTitle2)) {
                    found[0] = true;
                }
                pathList.add(new ArrayList<>(Arrays.asList(pageTitle1, link)));
            }
            Set<String> tempPreviousNodes = Collections.synchronizedSet(new HashSet<>());
            Set<ArrayList<String>> tempPathList = new HashSet<>(pathList);
            int endLayer = 1;

            while(!found[0]) {
                endLayer++;
                previousNodes.addAll(tempPreviousNodes);
                tempPreviousNodes.clear();
                pathList = new HashSet<>(tempPathList);
                tempPathList.clear();
                List<ArrayList<String>> tempPathListLocal = Collections.synchronizedList(new ArrayList<>());

                pathList.parallelStream().takeWhile(list -> !exit.get()).forEach(list -> {
                    ArrayList<String> linkList = wiki.getLinksOnPage(list.get(list.size()-1));
                    linkList.parallelStream().takeWhile(link -> !exit.get()).forEach(link -> {
                        if (link.equals(pageTitle2)) {
                            found[0] = true;
                            ArrayList<String> temp = new ArrayList<>(list);
                            temp.add(link);
                            tempPathListLocal.add(temp);
                        }
                        if (!previousNodes.contains(link)) {
                            tempPreviousNodes.add(link);
                            ArrayList<String> temp = new ArrayList<>(list);
                            temp.add(link);
                            tempPathListLocal.add(temp);
                        }
                    });
                });

                tempPathList.addAll(tempPathListLocal);
            }

            pathList = new HashSet<>(tempPathList);

            for (ArrayList<String> path : pathList) {
                if (!path.get(path.size() - 1).equals(pageTitle2)) {
                    tempPathList.remove(path);
                }
            }

            pathList = new HashSet<>(tempPathList);
            String tempWord;
            int layerCount = 1;
            while (layerCount < endLayer) {
                String smallestWord = null;
                for (ArrayList<String> path : pathList) {
                    tempWord = path.get(layerCount);
                    if (smallestWord == null) {
                        smallestWord = tempWord;
                    } else {
                        if (smallestWord.compareTo(tempWord) < 0) {
                            tempPathList.remove(path);
                        } else {
                            smallestWord = tempWord;
                        }
                    }
                }

                pathList = new HashSet<>(tempPathList);
                for (ArrayList<String> path : pathList) {
                    if (smallestWord.compareTo(path.get(layerCount)) < 0) {
                        tempPathList.remove(path);
                    }
                }

                pathList = new HashSet<>(tempPathList);
                layerCount++;
            }

            for (ArrayList<String> path : pathList) {
                return path;
            }

            return new ArrayList<>();
        });

        try {
            return future.get(timeout, TimeUnit.SECONDS);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException("Error in finding shortest path.");
        } catch (TimeoutException te) {
            exit.set(true);
            throw new TimeoutException();
        }
    }

    /**
     * Stores all search/getPage requests and their corresponding times, as well as
     * times of all requests, into local files.
     */
    public void storeRequests() {
        try {
            File searchGetFile = new File("./local/pastSearchGetRequests.ser");
            searchGetFile.delete();
            searchGetFile.createNewFile();

            File reqTimesFile = new File("./local/requestTimes.ser");
            reqTimesFile.delete();
            reqTimesFile.createNewFile();

            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(searchGetFile));
            out.writeObject(pastSearchGetRequests);

            out = new ObjectOutputStream(new FileOutputStream(reqTimesFile));
            out.writeObject(requestTimes);

            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Can't access filesystem.");
        }
    }

    /**
     * Reads files containing all search/getPage requests and their corresponding times, as well as
     * times of all requests received, and loads them into the corresponding values of the WikiMediator
     * object.
     */
    public void readStorage() {
        File searchGetFile = new File("./local/pastSearchGetRequests.ser");
        File reqTimesFile = new File("./local/requestTimes.ser");
        ObjectInputStream in;

        try {
            if (searchGetFile.isFile()) {
                in = new ObjectInputStream(new FileInputStream(searchGetFile));
                pastSearchGetRequests = (Map<String, List<Long>>) in.readObject();
                in.close();
            }
            if (reqTimesFile.isFile()) {
                in = new ObjectInputStream(new FileInputStream(reqTimesFile));
                requestTimes = (List<Long>) in.readObject();
                in.close();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

    }
}
